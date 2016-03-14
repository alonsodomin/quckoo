package io.kairos.console.server

import io.kairos.console.model.ExecutionPlanDetails
import io.kairos.id._

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait SchedulerFacade {

  def executionPlan(planId: PlanId): Future[ExecutionPlanDetails]

  def allExecutionPlanIds: Future[List[PlanId]]

}
