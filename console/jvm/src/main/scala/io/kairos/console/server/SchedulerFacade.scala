package io.kairos.console.server

import io.kairos.console.model.Schedule
import io.kairos.id._

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait SchedulerFacade {

  def executionPlan(planId: PlanId): Future[Schedule]

  def allExecutionPlanIds: Future[List[PlanId]]

}
