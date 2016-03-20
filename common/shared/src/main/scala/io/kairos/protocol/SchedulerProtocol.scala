package io.kairos.protocol

import io.kairos.{Task, JobSpec, Trigger}
import io.kairos.Trigger.Immediate
import io.kairos.id._
import io.kairos.time.DateTime
import monocle.macros.Lenses

import scala.concurrent.duration.FiniteDuration

/**
 * Created by domingueza on 22/08/15.
 */
object SchedulerProtocol {

  final val SchedulerTopic = "Scheduler"

  sealed trait SchedulerMessage
  sealed trait SchedulerCommand extends SchedulerMessage
  sealed trait SchedulerEvent extends SchedulerMessage

  @Lenses
  case class ScheduleJob(jobId: JobId,
      //params: Map[String, String] = Map.empty,
      trigger: Trigger = Immediate,
      timeout: Option[FiniteDuration] = None
  ) extends SchedulerCommand

  case class TaskScheduled(jobId: JobId, planId: PlanId, taskId: TaskId) extends SchedulerEvent
  case class TaskCompleted(jobId: JobId, planId: PlanId, taskId: TaskId, outcome: Task.Outcome) extends SchedulerEvent

  case class JobNotFound(jobId: JobId) extends SchedulerMessage
  case class JobNotEnabled(jobId: JobId) extends SchedulerMessage
  case class JobFailedToSchedule(jobId: JobId, cause: Throwable) extends SchedulerMessage

  case class ExecutionPlanStarted(jobId: JobId, planId: PlanId) extends SchedulerEvent
  case class ExecutionPlanFinished(jobId: JobId, planId: PlanId) extends SchedulerEvent

  case object GetExecutionPlans extends SchedulerCommand
  case class GetExecutionPlan(planId: PlanId) extends SchedulerCommand
  case class CancelPlan(planId: PlanId) extends SchedulerCommand

}
