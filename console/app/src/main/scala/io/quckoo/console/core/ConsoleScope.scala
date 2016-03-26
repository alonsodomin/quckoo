package io.quckoo.console.core

import diode.data._

import io.quckoo.auth.{AuthInfo, User}
import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.console.components.Notification
import io.quckoo.console.security.ClientAuth
import io.quckoo.id.{JobId, PlanId}

/**
  * Created by alonsodomin on 20/02/2016.
  */

case class ConsoleScope private (
    authInfo: Option[AuthInfo],
    notification: Option[Notification],
    jobSpecs: PotMap[JobId, JobSpec],
    executionPlans: PotMap[PlanId, ExecutionPlan]
) {

  def currentUser = authInfo.map(auth => User(auth.userId))

}

object ConsoleScope extends ClientAuth {

  def initial =
    ConsoleScope(
      authInfo       = super.authInfo,
      notification   = None,
      jobSpecs       = PotMap(JobSpecFetcher),
      executionPlans = PotMap(ExecutionPlanFetcher)
    )

}
