package io.quckoo.protocol.scheduler

import io.quckoo.{Task, Trigger}
import io.quckoo.Trigger.Immediate
import io.quckoo.id._
import monocle.macros.Lenses

import scala.concurrent.duration.FiniteDuration

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
case class TaskTriggered(jobId: JobId, planId: PlanId, taskId: TaskId) extends SchedulerEvent
case class TaskCompleted(jobId: JobId, planId: PlanId, taskId: TaskId, outcome: Task.Outcome) extends SchedulerEvent

case class JobNotFound(jobId: JobId) extends SchedulerMessage
case class JobNotEnabled(jobId: JobId) extends SchedulerMessage
case class JobFailedToSchedule(jobId: JobId, cause: Throwable) extends SchedulerMessage

case class ExecutionPlanStarted(jobId: JobId, planId: PlanId) extends SchedulerEvent
case class ExecutionPlanFinished(jobId: JobId, planId: PlanId) extends SchedulerEvent

case object GetExecutionPlans extends SchedulerCommand
case class GetExecutionPlan(planId: PlanId) extends SchedulerCommand
case class CancelPlan(planId: PlanId) extends SchedulerCommand