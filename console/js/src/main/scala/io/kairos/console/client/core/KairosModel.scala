package io.kairos.console.client.core

import diode.data._
import io.kairos.{ExecutionPlan, JobSpec}
import io.kairos.console.auth.User
import io.kairos.console.client.components.Notification
import io.kairos.console.client.security.ClientAuth
import io.kairos.id.{JobId, PlanId}

/**
  * Created by alonsodomin on 20/02/2016.
  */

case class KairosModel private (
    currentUser: Option[User],
    notification: Option[Notification],
    jobSpecs: PotMap[JobId, JobSpec],
    executionPlans: PotMap[PlanId, ExecutionPlan]
)

case class LoggedIn(username: String)
case object LoggedOut

object KairosModel extends ClientAuth {

  def initial =
    KairosModel(
      currentUser    = authInfo.map(auth => User(auth.userId)),
      notification   = None,
      jobSpecs       = PotMap(JobSpecFetcher),
      executionPlans = PotMap(ExecutionPlanFetcher)
    )

}
