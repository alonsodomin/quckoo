package io.quckoo.console.core

import diode.data.{Failed, Pot, Ready, Unavailable}
import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.client.QuckooClient
import io.quckoo.id._
import io.quckoo.net.ClusterState

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 25/03/2016.
  */
private[core] trait ConsoleOps {

  def refreshClusterStatus(implicit client: QuckooClient): Future[ClusterStateLoaded] =
    client.clusterState.map(ClusterStateLoaded(_))

  def loadJobSpec(jobId: JobId)(implicit client: QuckooClient): Future[(JobId, Pot[JobSpec])] =
    client.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty)(implicit client: QuckooClient): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      client.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  def loadPlanIds(implicit client: QuckooClient): Future[Set[PlanId]] = client.allExecutionPlanIds

  def loadPlans(ids: Set[PlanId])(implicit client: QuckooClient): Future[Map[PlanId, Pot[ExecutionPlan]]] = {
    Future.sequence(ids.map { id =>
      loadPlan(id).map(plan => id -> plan)
    }) map(_.toMap)
  }

  def loadPlan(id: PlanId)(implicit client: QuckooClient): Future[Pot[ExecutionPlan]] =
    client.executionPlan(id) map {
      case Some(plan) => Ready(plan)
      case None       => Unavailable
    } recover {
      case ex => Failed(ex)
    }

}
