package io.quckoo.console.client.core

import diode.data._
import io.quckoo.auth.User
import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.console.client.components.Notification
import io.quckoo.console.client.security.ClientAuth
import io.quckoo.id.{JobId, PlanId}

/**
  * Created by alonsodomin on 20/02/2016.
  */

case class ConsoleModel private(
    currentUser: Option[User],
    notification: Option[Notification],
    jobSpecs: PotMap[JobId, JobSpec],
    executionPlans: PotMap[PlanId, ExecutionPlan]
)

case class LoggedIn(username: String)
case object LoggedOut

object ConsoleModel extends ClientAuth {

  def initial =
    ConsoleModel(
      currentUser    = authInfo.map(auth => User(auth.userId)),
      notification   = None,
      jobSpecs       = PotMap(JobSpecFetcher),
      executionPlans = PotMap(ExecutionPlanFetcher)
    )

}
