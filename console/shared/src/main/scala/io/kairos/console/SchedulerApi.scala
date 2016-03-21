package io.kairos.console

import io.kairos.ExecutionPlan
import io.kairos.id._
import io.kairos.protocol.SchedulerProtocol

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait SchedulerApi {
  import SchedulerProtocol._

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]]

  def allExecutionPlanIds(implicit ec: ExecutionContext): Future[List[PlanId]]

  def schedule(scheduleJob: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]]

}
