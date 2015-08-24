package io.chronos.scheduler.queue

import akka.actor.{ActorLogging, ActorRef, Address, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.persistence.PersistentActor
import io.chronos.cluster.protocol.WorkerProtocol._
import io.chronos.cluster.{Task, WorkerId}
import io.chronos.id.TaskId
import io.chronos.scheduler.execution.Execution

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object TaskQueue {

  def props(maxWorkTimeout: FiniteDuration = 10 minutes) =
    Props(classOf[TaskQueue], maxWorkTimeout)

  case class Enqueue(task: Task)
  case class EnqueueAck(taskId: TaskId)
  case object GetWorkers
  case class Workers(locations: Seq[Address])

  final case class TimeOut(taskId: TaskId)

  private object WorkerState {
    sealed trait WorkerStatus

    case object Idle extends WorkerStatus
    case class Busy(taskId: TaskId, deadline: Deadline) extends WorkerStatus
  }

  private case class WorkerState(ref: ActorRef, status: WorkerState.WorkerStatus)

  private case object CleanupTick

}

class TaskQueue(maxWorkTimeout: FiniteDuration) extends PersistentActor with ActorLogging {
  import TaskQueue._
  import WorkerState._

  ClusterClientReceptionist(context.system).registerService(self)

  // Persistent state
  private var state = TaskQueueState.empty

  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-queue"
    case None       => "queue"
  }

  // Non-persistent state
  private var workers = Map.empty[WorkerId, WorkerState]

  import context.dispatcher
  private val cleanupTask = context.system.scheduler.schedule(maxWorkTimeout / 2, maxWorkTimeout / 2, self, CleanupTick)

  override def postStop(): Unit = cleanupTask.cancel()

  override def receiveRecover: Receive = {
    case event: TaskQueueState.TaskQueueEvent =>
      log.info("Replaying event: {}", event)
      state = state.updated(event)
  }

  override def receiveCommand: Receive = {
    case GetWorkers =>
      sender ! Workers(workers.values.map(_.ref.path.address).toSeq)

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered. workerId={}", workerId)
        if (state.hasPendingTasks) {
          sender ! TaskReady
        }
      }

    case RequestTask(workerId) if state.hasPendingTasks =>
      workers.get(workerId) match {
        case Some(workerState @ WorkerState(_, WorkerState.Idle)) =>
          val (task, execution) = state.nextTask
          persist(TaskQueueState.TaskStarted(task.id)) { event =>
            state = state.updated(event)
            val timeout = Deadline.now + maxWorkTimeout
            workers += (workerId -> workerState.copy(status = Busy(task.id, timeout)))
            log.info("Delivering execution to worker. taskId={}, workerId={}", task.id, workerId)
            workerState.ref ! task
            execution ! Execution.Start
          }

        case _ =>
          log.info("Receiver a request for tasks from a busy Worker. workerId={}", workerId)
      }

    case TaskDone(workerId, taskId, result) =>
      if (state.isDone(taskId)) {
        // resend Ack as probably the previous one was lost.
        sender ! TaskDoneAck(taskId)
      } else if (!state.isInProgress(taskId)) {
        log.warning("A worker has reported a non in-progress execution as done. workerId={}, taskId={}", workerId, taskId)
      } else {
        log.info("Execution finished by worker. workerId={}, taskId={}", workerId, taskId)
        changeWorkerToIdle(workerId, taskId)
        persist(TaskQueueState.TaskCompleted(taskId, result)) { event =>
          state = state.updated(event)
          state.executionOf(taskId) ! Execution.Finish(Right(result))
          sender ! TaskDoneAck(taskId)
          notifyWorkers()
        }
      }

    case TaskFailed(workerId, taskId, cause) if state.isInProgress(taskId) =>
      log.error("Worker failed executing given task. workerId={}, taskId={}", workerId, taskId)
      changeWorkerToIdle(workerId, taskId)
      persist(TaskQueueState.TaskFailed(taskId, cause)) { event =>
        state = state.updated(event)
        state.executionOf(taskId) ! Execution.Finish(Left(cause))
        notifyWorkers()
      }

    case Enqueue(task) =>
      if (state.isAccepted(task.id)) {
        sender ! EnqueueAck(task.id)
      } else {
        log.info("Accepted task. taskId={}", task.id)
        persist(TaskQueueState.TaskAccepted(task, sender())) { event =>
          state = state.updated(event)
          sender ! EnqueueAck(task.id)
          notifyWorkers()
        }
      }

    case TimeOut(taskId) =>
      for ((workerId, s @ WorkerState(_, Busy(`taskId`, _))) <- workers) {
        timeoutWorker(workerId, taskId)
      }

    case CleanupTick =>
      for ((workerId, s @ WorkerState(_, Busy(taskId, timeout))) <- workers) {
        if (timeout.isOverdue()) timeoutWorker(workerId, taskId)
      }
  }

  private def notifyWorkers(): Unit = if (state.hasPendingTasks) {
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

  private def timeoutWorker(workerId: WorkerId, taskId: TaskId): Unit = {
    workers -= workerId
    persist(TaskQueueState.TaskTimedOut(taskId)) { event =>
      state = state.updated(event)
      state.executionOf(taskId) ! Execution.TimeOut
      notifyWorkers()
    }
  }

}
