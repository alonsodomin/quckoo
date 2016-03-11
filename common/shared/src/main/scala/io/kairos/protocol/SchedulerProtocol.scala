package io.kairos.protocol

import io.kairos.Trigger
import io.kairos.Trigger.{LastExecutionTime, Immediate}
import io.kairos.id._
import io.kairos.time.DateTime
import monocle.macros.Lenses

import scala.concurrent.duration.FiniteDuration

/**
 * Created by domingueza on 22/08/15.
 */
object SchedulerProtocol {

  final val SchedulerTopic = "Scheduler"

  sealed trait SchedulerCommand
  sealed trait SchedulerEvent

  @Lenses
  case class ScheduleJob(jobId: JobId,
      params: Map[String, AnyVal] = Map.empty,
      trigger: Trigger = Immediate,
      timeout: Option[FiniteDuration] = None
  ) extends SchedulerCommand

  case class JobScheduled(jobId: JobId, planId: PlanId) extends SchedulerEvent
  case class JobFailedToSchedule(jobId: JobId, cause: Throwable) extends SchedulerEvent

  case object GetExecutionPlans extends SchedulerCommand
  case class CancelPlan(planId: PlanId) extends SchedulerCommand

  case class ExecutionPlanDetails(
      jobId: JobId,
      params: Map[String, AnyVal],
      trigger: Trigger,
      lastExecution: DateTime
  )

}
