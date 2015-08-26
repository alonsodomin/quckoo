package io.chronos.scheduler

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import io.chronos.cluster.protocol.WorkerProtocol
import io.chronos.cluster.{Task, WorkerId}
import io.chronos.id.TaskId
import io.chronos.scheduler.execution.Execution

import scala.collection.immutable.Queue
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object TaskQueue {

  type AcceptedTask = (Task, ActorRef)

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
  import WorkerProtocol._
  import WorkerState._

  private val mediator = DistributedPubSub(context.system).mediator

  private var workers = Map.empty[WorkerId, WorkerState]
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
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered. workerId={}, location={}", workerId, sender().path.address)
        mediator ! DistributedPubSubMediator.Publish(WorkerTopic, WorkerJoined(workerId))
        if (pendingTasks.nonEmpty) {
          sender ! TaskReady
        }
      }

    case RequestTask(workerId) if pendingTasks.nonEmpty =>
      workers.get(workerId) match {
        case Some(workerState @ WorkerState(_, WorkerState.Idle)) =>
          def dispatchTask(task: Task, executionActor: ActorRef): Unit = {
            val timeout = Deadline.now + maxWorkTimeout
            workers += (workerId -> workerState.copy(status = Busy(task.id, timeout)))
            log.info("Delivering execution to worker. taskId={}, workerId={}", task.id, workerId)
            workerState.ref ! task
            executionActor ! Execution.Start
            inProgressTasks += (task.id -> executionActor)
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
        inProgressTasks(taskId) ! Execution.Finish(Right(result))
        inProgressTasks -= taskId
        sender ! TaskDoneAck(taskId)
        notifyWorkers()
      }

    case TaskFailed(workerId, taskId, cause) if inProgressTasks.contains(taskId) =>
      log.error("Worker failed executing given task. workerId={}, taskId={}", workerId, taskId)
      changeWorkerToIdle(workerId, taskId)
      inProgressTasks(taskId) ! Execution.Finish(Left(cause))
      inProgressTasks -= taskId
      notifyWorkers()

    case Enqueue(task) =>
      // Enqueue messages will always come from inside the cluster so accept them all
      pendingTasks = pendingTasks.enqueue(task, sender())
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
  }

  private def notifyWorkers(): Unit = if (pendingTasks.nonEmpty) {
    def randomWorkers: Seq[(WorkerId, WorkerState)] = workers.toSeq

    randomWorkers.foreach {
      case (_, WorkerState(ref, WorkerState.Idle)) => ref ! TaskReady
      case _ => // busy
    }
  }

  private def changeWorkerToIdle(workerId: WorkerId, taskId: TaskId): Unit =
    workers.get(workerId) match {
      case Some(s @ WorkerState(_, WorkerState.Busy(`taskId`, _))) =>
        workers += (workerId -> s.copy(status = WorkerState.Idle))
      case _ =>
      // ok, might happen after standby recovery, worker state is not persisted
    }

  private def timeoutWorker(workerId: WorkerId, taskId: TaskId): Unit = if (inProgressTasks.contains(taskId)) {
    workers -= workerId
    inProgressTasks(taskId) ! Execution.TimeOut
    inProgressTasks -= taskId
    mediator ! DistributedPubSubMediator.Publish(WorkerTopic, WorkerRemoved(workerId))
    notifyWorkers()
  }

}
