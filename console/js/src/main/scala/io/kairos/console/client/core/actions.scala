package io.kairos.console.client.core

import diode.data.{AsyncAction, Pot, PotState}
import io.kairos._
import io.kairos.console.model.Schedule
import io.kairos.id.{JobId, PlanId}

import scala.util.{Failure, Try}

case object LoadJobSpecs
case class JobSpecsLoaded(value: Map[JobId, Pot[JobSpec]])

case class UpdateJobSpecs(
    keys: Set[JobId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[JobId, Pot[JobSpec]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[JobId, Pot[JobSpec]], UpdateJobSpecs] {

  override def next(newState: PotState, newValue: Try[Map[JobId, Pot[JobSpec]]]): UpdateJobSpecs =
    copy(state = newState, result = newValue)

}

case class RegisterJob(spec: JobSpec)
case class RegisterJobResult(jobId: Validated[JobId])

case object LoadSchedules
case class ScheduleIds(planIds: Set[PlanId])

case class UpdateSchedules(
    keys: Set[PlanId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[PlanId, Pot[Schedule]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[PlanId, Pot[Schedule]], UpdateSchedules] {

  override def next(newState: PotState, newValue: Try[Map[PlanId, Pot[Schedule]]]): UpdateSchedules =
    copy(state = newState, result = newValue)

}