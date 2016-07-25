package io.quckoo

import io.quckoo.fault.Fault
import io.quckoo.id.PlanId

/**
  * Created by alonsodomin on 24/07/2016.
  */
final case class Execution(
  planId: PlanId,
  task: Task,
  status: Execution.Status,
  outcome: Option[Execution.Outcome] = None
)
object Execution {
  sealed trait Status
  case object Scheduled extends Status
  case object Enqueued extends Status
  case object InProgress extends Status
  case object Complete extends Status

  sealed trait UncompletedReason
  case object UserRequest extends UncompletedReason
  case object FailedToEnqueue extends UncompletedReason

  sealed trait Outcome
  case object Success extends Outcome
  final case class Failure(cause: Fault) extends Outcome
  final case class Interrupted(reason: UncompletedReason) extends Outcome
  final case class NeverRun(reason: UncompletedReason) extends Outcome
  case object NeverEnding extends Outcome
}
