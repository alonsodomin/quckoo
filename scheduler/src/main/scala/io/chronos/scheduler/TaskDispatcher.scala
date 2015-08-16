package io.chronos.scheduler

import java.util.UUID

import akka.actor.ActorLogging
import akka.cluster.Cluster
import akka.persistence.PersistentActor
import io.chronos.JobClass
import io.chronos.cluster.WorkerId
import io.chronos.cluster.WorkerProtocol.{RegisterWorker, RequestWork, WorkReady}
import io.chronos.id.ModuleId

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Deadline, FiniteDuration}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object TaskDispatcher {

  type TaskId = UUID

  case class Task(id: TaskId, moduleId: ModuleId, params: Map[String, AnyVal],
                  jobClass: JobClass, timeout: Option[FiniteDuration])

  case class TaskAck(taskId: TaskId)

  sealed trait DispatcherEvent
  case class TaskAccepted(task: Task) extends DispatcherEvent
  case class TaskStarted(taskId: TaskId, workerId: WorkerId)

}

class TaskDispatcher(maxWorkTimeout: FiniteDuration) extends PersistentActor with ActorLogging {
  import TaskDispatcher._

  // persistenceId must include cluster role to support multiple masters
  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-dispatcher"
    case None       => "dispatcher"
  }

  private var acceptedTasks = Map.empty[TaskId, Task]
  private var pendingTasks = Queue.empty[Task]

  // Non-persistent state
  private var workers = Map.empty[WorkerId, WorkerState]

  override def receiveRecover: Receive = {
    case event: DispatcherEvent =>
      log.debug("Replayed event. event={}", event)
      updateState(event)
  }

  override def receiveCommand: Receive = {
    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered. workerId={}", workerId)
        if (pendingTasks.nonEmpty) {
          sender ! WorkReady
        }
      }

    case RequestWork(workerId) if pendingTasks.nonEmpty =>
      workers.get(workerId) match {
        case Some(worker @ WorkerState(_, WorkerState.Idle)) =>
          val (task, rest) = pendingTasks.dequeue
          persist(TaskStarted(task.id, workerId)) { event =>
            pendingTasks = rest
            val timeout = Deadline.now + (task.timeout.getOrElse(maxWorkTimeout))
            //workers += (workerId -> worker.copy(status = Busy))
          }
      }

    case task: Task =>
      if (acceptedTasks.contains(task.id)) {
        sender() ! TaskAck(task.id)
      } else {
        log.debug("Accepted task. taskId={}", task.id)
        persist(TaskAccepted(task)) { event =>
          updateState(event)

          notifyWorkers()
        }
      }
  }

  private def updateState(event: DispatcherEvent): Unit = event match {
    case TaskAccepted(task) =>
      acceptedTasks += (task.id -> task)
      pendingTasks = pendingTasks enqueue task
  }

  private def notifyWorkers(): Unit = if (pendingTasks.nonEmpty) {
    def randomWorkers: Seq[(WorkerId, WorkerState)] = workers.toSeq

    randomWorkers.foreach {
      case (_, WorkerState(ref, WorkerState.Idle)) => ref ! WorkReady
      case _ => // busy
    }
  }

}
