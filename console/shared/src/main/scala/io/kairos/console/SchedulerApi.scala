package io.kairos.console

import io.kairos.console.model.Schedule
import io.kairos.id._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait SchedulerApi {

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Schedule]

  def allExecutionPlanIds(implicit ec: ExecutionContext): Future[List[PlanId]]

}
