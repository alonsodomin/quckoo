/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.scheduler

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ddata._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import io.quckoo.Task
import io.quckoo.cluster.protocol._
import io.quckoo.cluster.topics
import io.quckoo.cluster.net._
import io.quckoo.id.{NodeId, TaskId}
import io.quckoo.protocol.worker._

import scala.collection.immutable.Queue
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object TaskQueue {

  type AcceptedTask = (Task, ActorRef)

  val PendingKey = PNCounterMapKey("pendingCount")
  val InProgressKey = PNCounterMapKey("inProgressCount")

  def props(maxWorkTimeout: FiniteDuration = 10 minutes) =
    Props(classOf[TaskQueue], maxWorkTimeout)

  case class Enqueue(task: Task)
  case class EnqueueAck(taskId: TaskId)
  case object GetWorkers
  case class Workers(locations: Seq[Address])

  case class TimeOut(taskId: TaskId)

  private object WorkerState {
    sealed trait WorkerStatus

    case object Idle extends WorkerStatus
    case class Busy(taskId: TaskId, deadline: Deadline) extends WorkerStatus
  }

  private case class WorkerState(ref: ActorRef, status: WorkerState.WorkerStatus)

  private case object CleanupTick

}

class TaskQueue(maxWorkTimeout: FiniteDuration) extends Actor with ActorLogging {
  import TaskQueue._
  import WorkerState._

  implicit val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator
  private val replicator = DistributedData(context.system).replicator

  private var workers = Map.empty[NodeId, WorkerState]
  private var pendingTasks = Queue.empty[AcceptedTask]
  private var inProgressTasks = Map.empty[TaskId, ActorRef]

  import context.dispatcher
  private val cleanupTask = context.system.scheduler.schedule(maxWorkTimeout / 2, maxWorkTimeout / 2, self, CleanupTick)

  override def postStop(): Unit = cleanupTask.cancel()

  override def receive: Receive = {
    case GetWorkers =>
      sender ! Workers(workers.values.map(_.ref.path.address).toSeq)

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        context.unwatch(workers(workerId).ref)
        workers += (workerId -> workers(workerId).copy(ref = sender()))
        context.watch(sender())
      } else {
        val workerRef = sender()
        workers += (workerId -> WorkerState(workerRef, status = WorkerState.Idle))
        context.watch(workerRef)

        val workerLocation = workerRef.location
        log.info("Worker registered. workerId={}, location={}", workerId, workerLocation)
        mediator ! DistributedPubSubMediator.Publish(topics.Worker, WorkerJoined(workerId, workerLocation))
        if (pendingTasks.nonEmpty) {
          sender ! TaskReady
        }
      }

    case RequestTask(workerId) if pendingTasks.nonEmpty =>
      workers.get(workerId) match {
        case Some(workerState @ WorkerState(_, WorkerState.Idle)) =>
          def dispatchTask(task: Task, lifecycle: ActorRef): Unit = {
            val timeout = Deadline.now + maxWorkTimeout
            workers += (workerId -> workerState.copy(status = Busy(task.id, timeout)))
            inProgressTasks += (task.id -> lifecycle)

            log.info("Delivering execution to worker. taskId={}, workerId={}", task.id, workerId)
            workerState.ref ! task
            lifecycle ! ExecutionLifecycle.Start

            replicator ! Replicator.Update(PendingKey, PNCounterMap(), Replicator.WriteLocal) {
              _.decrement(cluster.selfUniqueAddress.toNodeId.toString)
            }
            replicator ! Replicator.Update(InProgressKey, PNCounterMap(), Replicator.WriteLocal) {
              _.increment(cluster.selfUniqueAddress.toNodeId.toString)
            }
          }

          def dequeueTask: Queue[AcceptedTask] = {
            val ((task, execution), remaining) = pendingTasks.dequeue
            dispatchTask(task, execution)
            remaining
          }

          pendingTasks = dequeueTask

        case _ =>
          log.info("Receiver a request for tasks from a busy Worker. workerId={}", workerId)
      }

    case TaskDone(workerId, taskId, result) =>
      if (!inProgressTasks.contains(taskId)) {
        // Assume that previous Ack was lost so resend it again
        sender ! TaskDoneAck(taskId)
      } else {
        log.info("Execution finished by worker. workerId={}, taskId={}", workerId, taskId)
        changeWorkerToIdle(workerId, taskId)
        inProgressTasks(taskId) ! ExecutionLifecycle.Finish(None)
        inProgressTasks -= taskId

        sender ! TaskDoneAck(taskId)
        replicator ! Replicator.Update(InProgressKey, PNCounterMap(), Replicator.WriteLocal) {
          _.decrement(cluster.selfUniqueAddress.toNodeId.toString)
        }
        notifyWorkers()
      }

    case TaskFailed(workerId, taskId, cause) if inProgressTasks.contains(taskId) =>
      log.error("Worker failed executing given task. workerId={}, taskId={}", workerId, taskId)
      changeWorkerToIdle(workerId, taskId)
      inProgressTasks(taskId) ! ExecutionLifecycle.Finish(Some(cause))
      inProgressTasks -= taskId
      replicator ! Replicator.Update(InProgressKey, PNCounterMap(), Replicator.WriteLocal) {
        _.decrement(cluster.selfUniqueAddress.toNodeId.toString)
      }
      notifyWorkers()

    case Enqueue(task) =>
      // Enqueue messages will always come from inside the cluster so accept them all
      log.debug("Enqueueing task {} before sending to workers.", task.id)
      pendingTasks = pendingTasks.enqueue((task, sender()))
      replicator ! Replicator.Update(PendingKey, PNCounterMap(), Replicator.WriteLocal) {
        _.increment(cluster.selfUniqueAddress.toNodeId.toString)
      }
      sender ! EnqueueAck(task.id)
      notifyWorkers()

    case TimeOut(taskId) =>
      for ((workerId, s @ WorkerState(_, Busy(`taskId`, _))) <- workers) {
        timeoutWorker(workerId, taskId)
      }

    case CleanupTick =>
      for ((workerId, s @ WorkerState(_, Busy(taskId, timeout))) <- workers) {
        if (timeout.isOverdue()) timeoutWorker(workerId, taskId)
      }

    case Terminated(workerRef) =>
      workers.find {
        case (_, WorkerState(`workerRef`, _)) => true
        case _ => false
      } foreach { case (workerId, workerState) =>
        log.info("Worker terminated! workerId={}", workerId)
        workers -= workerId
        workerState.status match {
          case WorkerState.Busy(taskId, _) =>
            // TODO define a better message to interrupt the execution
            inProgressTasks(taskId) ! ExecutionLifecycle.TimeOut
            inProgressTasks -= taskId
            replicator ! Replicator.Update(InProgressKey, PNCounterMap(), Replicator.WriteLocal) {
              _.decrement(cluster.selfUniqueAddress.toNodeId.toString)
            }

          case _ =>
        }
        mediator ! DistributedPubSubMediator.Publish(topics.Worker, WorkerLost(workerId))
      }
  }

  override def unhandled(message: Any): Unit = message match {
    case Replicator.UpdateSuccess(PendingKey, _) => // ignored
    case _ => super.unhandled(message)
  }

  private def notifyWorkers(): Unit = if (pendingTasks.nonEmpty) {
    def randomWorkers: Seq[(NodeId, WorkerState)] = workers.toSeq

    randomWorkers.foreach {
      case (_, WorkerState(ref, WorkerState.Idle)) => ref ! TaskReady
      case _ => // busy
    }
  }

  private def changeWorkerToIdle(workerId: NodeId, taskId: TaskId): Unit =
    workers.get(workerId) match {
      case Some(s @ WorkerState(_, WorkerState.Busy(`taskId`, _))) =>
        workers += (workerId -> s.copy(status = WorkerState.Idle))
      case _ =>
      // ok, might happen after standby recovery, worker state is not persisted
    }

  private def timeoutWorker(workerId: NodeId, taskId: TaskId): Unit = if (inProgressTasks.contains(taskId)) {
    workers -= workerId
    inProgressTasks(taskId) ! ExecutionLifecycle.TimeOut
    inProgressTasks -= taskId
    mediator ! DistributedPubSubMediator.Publish(topics.Worker, WorkerRemoved(workerId))
    replicator ! Replicator.Update(InProgressKey, PNCounterMap(), Replicator.WriteLocal) {
      _.decrement(cluster.selfUniqueAddress.toNodeId.toString)
    }
    notifyWorkers()
  }

}
