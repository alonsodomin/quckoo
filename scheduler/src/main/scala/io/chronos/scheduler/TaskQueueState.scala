package io.chronos.scheduler

import akka.actor.ActorRef
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.id._

import scala.collection.immutable.Queue

/**
 * Created by aalonsodominguez on 17/08/15.
 */
object TaskQueueState {

  type AcceptedTask = (Task, ActorRef)

  def empty: TaskQueueState = new TaskQueueState(Map.empty, Queue.empty, Set.empty, Set.empty)

  sealed trait TaskQueueEvent
  case class TaskAccepted(task: Task, execution: ActorRef) extends TaskQueueEvent
  case class TaskStarted(taskId: TaskId) extends TaskQueueEvent
  case class TaskCompleted(taskId: TaskId, result: AnyVal) extends TaskQueueEvent
  case class TaskFailed(taskId: TaskId, cause: TaskFailureCause) extends TaskQueueEvent
  case class TaskTimedOut(taskId: TaskId) extends TaskQueueEvent

}

case class TaskQueueState private (
    private val acceptedTasks: Map[TaskId, TaskQueueState.AcceptedTask],
    private val pendingTasks: Queue[TaskQueueState.AcceptedTask],
    private val inProgressTasks: Set[TaskId],
    private val doneTasks: Set[TaskId]) {

  import TaskQueueState._

  def hasPendingTasks: Boolean = pendingTasks.nonEmpty
  def nextTask: AcceptedTask = pendingTasks.head
  def isAccepted(taskId: TaskId): Boolean = acceptedTasks.contains(taskId)
  def isInProgress(taskId: TaskId): Boolean = inProgressTasks.contains(taskId)
  def isDone(taskId: TaskId): Boolean = doneTasks.contains(taskId)
  def executionOf(taskId: TaskId): ActorRef = acceptedTasks(taskId)._2

  def updated(event: TaskQueueEvent): TaskQueueState = event match {
    case TaskAccepted(task, execution) =>
      copy(acceptedTasks = acceptedTasks + (task.id -> (task, execution)),
        pendingTasks = pendingTasks enqueue (task, execution))

    case TaskStarted(taskId) =>
      val (_, rest) = pendingTasks.dequeue
      copy(pendingTasks = rest, inProgressTasks = inProgressTasks + taskId)

    case TaskCompleted(taskId, _) =>
      copy(inProgressTasks = inProgressTasks - taskId, doneTasks = doneTasks + taskId)

    case TaskFailed(taskId, _) =>
      copy(inProgressTasks = inProgressTasks - taskId)

    case TaskTimedOut(taskId) =>
      copy(inProgressTasks = inProgressTasks - taskId)
  }

}
