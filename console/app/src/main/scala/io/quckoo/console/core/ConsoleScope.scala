package io.quckoo.console.core

import diode.data._
import io.quckoo.client.QuckooClient
import io.quckoo.client.ajax.AjaxQuckooClientFactory
import io.quckoo.console.components.Notification
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.{ExecutionPlan, JobSpec}

/**
  * Created by alonsodomin on 20/02/2016.
  */

case class ConsoleScope private (
    client: Option[QuckooClient],
    notification: Option[Notification],
    jobSpecs: PotMap[JobId, JobSpec],
    executionPlans: PotMap[PlanId, ExecutionPlan]
) {

  def currentUser = client.map(_.principal)

}

object ConsoleScope {

  def initial =
    ConsoleScope(
      client         = None,
      notification   = None,
      jobSpecs       = PotMap(JobSpecFetcher),
      executionPlans = PotMap(ExecutionPlanFetcher)
    )

}
