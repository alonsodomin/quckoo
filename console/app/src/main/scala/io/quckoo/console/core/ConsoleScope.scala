package io.quckoo.console.core

import diode.data._
import io.quckoo.client.QuckooClient
import io.quckoo.client.ajax.AjaxQuckooClientFactory
import io.quckoo.console.components.Notification
import io.quckoo.id.{JobId, PlanId, WorkerId}
import io.quckoo.protocol.cluster.ClusterInfo
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.{ExecutionPlan, JobSpec}

/**
  * Created by alonsodomin on 20/02/2016.
  */

case class ConsoleScope private (
    client: Option[QuckooClient],
    notification: Option[Notification],
    clusterState: ClusterInfo,
    jobSpecs: PotMap[JobId, JobSpec],
    executionPlans: PotMap[PlanId, ExecutionPlan]
) {

  def currentUser = client.flatMap(_.principal)

}

object ConsoleScope {

  def initial =
    ConsoleScope(
      client         = AjaxQuckooClientFactory.autoConnect(),
      notification   = None,
      clusterState   = ClusterInfo(),
      jobSpecs       = PotMap(JobSpecFetcher),
      executionPlans = PotMap(ExecutionPlanFetcher)
    )

}
