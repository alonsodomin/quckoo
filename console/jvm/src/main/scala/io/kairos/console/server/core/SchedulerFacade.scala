package io.kairos.console.server.core

import io.kairos.ExecutionPlan
import io.kairos.id._
import io.kairos.protocol.SchedulerProtocol

import scala.concurrent.Future

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait SchedulerFacade {
  import SchedulerProtocol._

  def executionPlan(planId: PlanId): Future[ExecutionPlan]

  def allExecutionPlanIds: Future[List[PlanId]]

  def schedule(schedule: ScheduleJob): Future[Either[JobNotFound, ExecutionPlanStarted]]

}
