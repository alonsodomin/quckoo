package io.quckoo.api

import io.quckoo.ExecutionPlan
import io.quckoo.id.PlanId
import io.quckoo.protocol.registry.JobNotFound
import io.quckoo.protocol.scheduler._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait Scheduler {

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]]

  def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]]

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]]

}
