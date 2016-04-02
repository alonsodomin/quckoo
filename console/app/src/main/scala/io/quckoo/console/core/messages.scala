package io.quckoo.console.core

import diode.data.{AsyncAction, Pot, PotState}

import io.quckoo._
import io.quckoo.client.QuckooClient
import io.quckoo.console.ConsoleRoute
import io.quckoo.id.{JobId, PlanId}

import scala.util.{Failure, Try}

case class Login(username: String, password: String, referral: Option[ConsoleRoute] = None)
case class LoggedIn(client: QuckooClient, referral: Option[ConsoleRoute])

case object Logout
case object LoggedOut
case object LoginFailed

case class NavigateTo(route: ConsoleRoute)

case object LoadJobSpecs
case class JobSpecsLoaded(value: Map[JobId, Pot[JobSpec]])

case class RefreshJobSpecs(
    keys: Set[JobId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[JobId, Pot[JobSpec]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[JobId, Pot[JobSpec]], RefreshJobSpecs] {

  override def next(newState: PotState, newValue: Try[Map[JobId, Pot[JobSpec]]]): RefreshJobSpecs =
    copy(state = newState, result = newValue)

}

case class RegisterJobResult(jobId: Validated[JobId])

case object LoadExecutionPlans
case class ExecutionPlanIdsLoaded(planIds: Set[PlanId])
case class ExecutionPlansLoaded(plans: Map[PlanId, Pot[ExecutionPlan]])

case class RefreshExecutionPlans(
    keys: Set[PlanId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[PlanId, Pot[ExecutionPlan]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[PlanId, Pot[ExecutionPlan]], RefreshExecutionPlans] {

  override def next(newState: PotState, newValue: Try[Map[PlanId, Pot[ExecutionPlan]]]): RefreshExecutionPlans =
    copy(state = newState, result = newValue)

}

case class SubscribeToBackend(client: QuckooClient)