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
final case class ScheduleJob(
    jobId: JobId,
    //params: Map[String, String] = Map.empty,
    trigger: Trigger = Immediate,
    timeout: Option[FiniteDuration] = None
) extends SchedulerCommand

final case class TaskScheduled(jobId: JobId, planId: PlanId, taskId: TaskId) extends SchedulerEvent
final case class TaskTriggered(jobId: JobId, planId: PlanId, taskId: TaskId) extends SchedulerEvent
final case class TaskCompleted(jobId: JobId, planId: PlanId, taskId: TaskId, outcome: Task.Outcome) extends SchedulerEvent

final case class JobNotFound(jobId: JobId) extends SchedulerMessage
final case class JobNotEnabled(jobId: JobId) extends SchedulerMessage
final case class JobFailedToSchedule(jobId: JobId, cause: Throwable) extends SchedulerMessage

final case class ExecutionPlanStarted(jobId: JobId, planId: PlanId) extends SchedulerEvent
final case class ExecutionPlanFinished(jobId: JobId, planId: PlanId) extends SchedulerEvent

case object GetExecutionPlans extends SchedulerCommand
final case class GetExecutionPlan(planId: PlanId) extends SchedulerCommand
final case class CancelPlan(planId: PlanId) extends SchedulerCommand

final case class TaskQueueUpdated(pendingTasks: Int, inProgressTasks: Int) extends SchedulerEvent