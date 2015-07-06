package io.chronos.scheduler.worker

import scala.collection.immutable.Queue

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object ExecutionPlan {

  def empty: ExecutionPlan = ExecutionPlan(
    pendingWork = Queue.empty,
    workInProgress = Map.empty,
    acceptedWorks = Set.empty,
    finishedWorkIds = Set.empty
  )

  trait WorkDomainEvent

  case class WorkAccepted(work: Work) extends WorkDomainEvent
  case class WorkStarted(workId: String) extends WorkDomainEvent
  case class WorkCompleted(workId: String, result: Any) extends WorkDomainEvent

  case class WorkerFailed(workId: String) extends WorkDomainEvent
  case class WorkerTimedOut(workId: String) extends WorkDomainEvent

}

case class ExecutionPlan private (
  private val pendingWork: Queue[Work],
  private val workInProgress: Map[String, Work],
  private val acceptedWorks: Set[String],
  private val finishedWorkIds: Set[String]) {

  import ExecutionPlan._

  def hasWork: Boolean = pendingWork.nonEmpty
  def nextWork: Work = pendingWork.head
  def isAccepted(workId: String): Boolean = acceptedWorks.contains(workId)
  def isInProgress(workId: String): Boolean = workInProgress.contains(workId)
  def isDone(workId: String): Boolean = finishedWorkIds.contains(workId)

  def updated(event: WorkDomainEvent): ExecutionPlan = event match {
    case WorkAccepted(work) =>
      copy(
        pendingWork = pendingWork enqueue work,
        acceptedWorks = acceptedWorks + work.workId
      )

    case WorkStarted(workId) =>
      val (work, rest) = pendingWork.dequeue
      require(work.workId == workId, s"WorkStarted expected workId $workId == ${work.workId}")
      copy(
        pendingWork = rest,
        workInProgress = workInProgress + (workId -> work)
      )

    case WorkCompleted(workId, _) =>
      copy(
        workInProgress = workInProgress - workId,
        finishedWorkIds = finishedWorkIds + workId
      )

    case WorkerFailed(workId) =>
      copy(
        pendingWork = pendingWork enqueue workInProgress(workId),
        workInProgress = workInProgress - workId
      )

    case WorkerTimedOut(workId) =>
      copy(
        pendingWork = pendingWork enqueue workInProgress(workId),
        workInProgress = workInProgress - workId
      )
  }

}
