package io.quckoo.console.client.core

import diode.data.{AsyncAction, Pot, PotState}
import io.quckoo._
import io.quckoo.auth.AuthInfo
import io.quckoo.console.client.SiteMap.ConsoleRoute
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.protocol.client.SignIn

import scala.util.{Failure, Try}

case class Login(signIn: SignIn, referral: Option[ConsoleRoute] = None)
case class LoggedIn(authInfo: AuthInfo, referral: Option[ConsoleRoute])
case object LoggedOut
case object LoginFailed

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