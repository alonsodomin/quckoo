package io.chronos.protocol

import io.chronos.Trigger
import io.chronos.Trigger.Immediate
import io.chronos.id._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by domingueza on 22/08/15.
 */
object SchedulerProtocol {

  sealed trait SchedulerCommand
  case class ScheduleJob(jobId: JobId,
                         params: Map[String, AnyVal] = Map.empty,
                         trigger: Trigger = Immediate,
                         timeout: Option[FiniteDuration] = None) extends SchedulerCommand

  case class JobScheduled(jobId: JobId, planId: PlanId)
  case class JobFailedToSchedule(jobId: JobId, cause: Throwable)

  case class CancelPlan(planId: PlanId)

}
