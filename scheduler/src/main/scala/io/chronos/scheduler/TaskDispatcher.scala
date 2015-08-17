package io.chronos.scheduler

import akka.actor.ActorLogging
import akka.cluster.Cluster
import akka.persistence.PersistentActor
import io.chronos.cluster.WorkerProtocol.{RegisterWorker, RequestWork, WorkReady}
import io.chronos.cluster.{Work, WorkerId}
import io.chronos.id.{ExecutionId, ModuleId}
import io.chronos.scheduler.WorkerState.Busy

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Deadline, FiniteDuration}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object TaskDispatcher {

  case class Task(id: ExecutionId, moduleId: ModuleId, params: Map[String, AnyVal],
                  jobClass: String, timeout: Option[FiniteDuration])

  case class TaskAck(taskId: ExecutionId)

  sealed trait DispatcherEvent
  case class TaskAccepted(task: Task) extends DispatcherEvent
  case class TaskStarted(taskId: ExecutionId, workerId: WorkerId) extends DispatcherEvent

  object State {

    def empty: State = new State(Map.empty, Queue.empty)

  }

  case class State private (
    private val acceptedTasks: Map[ExecutionId, Task],
    private val pendingTasks: Queue[Task]) {

    def hasPending: Boolean = pendingTasks.nonEmpty
    def next: Task = pendingTasks.head
    def isAccepted(executionId: ExecutionId): Boolean = acceptedTasks.contains(executionId)

    def updated(event: DispatcherEvent): State = event match {
      case TaskAccepted(task) =>
        copy(acceptedTasks = acceptedTasks + (task.id -> task),
          pendingTasks = pendingTasks enqueue task)

      case TaskStarted(taskId, _) =>
        val (_, rest) = pendingTasks.dequeue
        copy(pendingTasks = rest)
    }

  }

}

class TaskDispatcher(maxWorkTimeout: FiniteDuration) extends PersistentActor with ActorLogging {
  import TaskDispatcher._

  // persistenceId must include cluster role to support multiple masters
  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-dispatcher"
    case None       => "dispatcher"
  }

  private var state = State.empty

  // Non-persistent state
  private var workers = Map.empty[WorkerId, WorkerState]

  override def receiveRecover: Receive = {
    case event: DispatcherEvent =>
      log.debug("Replayed event. event={}", event)
      state = state.updated(event)
  }

  override def receiveCommand: Receive = {
    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered. workerId={}", workerId)
        if (state.hasPending) {
          sender ! WorkReady
        }
      }

    case RequestWork(workerId) if state.hasPending =>
      workers.get(workerId) match {
        case Some(worker @ WorkerState(_, WorkerState.Idle)) =>
          val task = state.next
          persist(TaskStarted(task.id, workerId)) { event =>
            state = state.updated(event)
            val timeout = Deadline.now + task.timeout.getOrElse(maxWorkTimeout)
            val work = Work(task.id, task.moduleId, task.params, task.jobClass)
            workers += (workerId -> worker.copy(status = Busy(task.id, timeout)))
            log.debug("Delivering execution to worker. executionId={}, workerId={}", task.id, workerId)
            worker.ref ! work
          }
      }

    case task: Task =>
      if (state.isAccepted(task.id)) {
        sender() ! TaskAck(task.id)
      } else {
        log.debug("Accepted task. taskId={}", task.id)
        persist(TaskAccepted(task)) { event =>
          state = state.updated(event)
          notifyWorkers()
        }
      }
  }

  private def notifyWorkers(): Unit = if (state.hasPending) {
    def randomWorkers: Seq[(WorkerId, WorkerState)] = workers.toSeq

    randomWorkers.foreach {
      case (_, WorkerState(ref, WorkerState.Idle)) => ref ! WorkReady
      case _ => // busy
    }
  }

}
