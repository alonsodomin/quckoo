package io.kairos

import io.kairos.id._
import io.kairos.time.{DateTime, TimeSource}
import monocle.macros.Lenses

/**
  * Created by alonsodomin on 14/03/2016.
  */
@Lenses final case class ExecutionPlan(
    jobId: JobId,
    planId: PlanId,
    trigger: Trigger,
    createdTime: DateTime,
    currentTaskId: Option[TaskId] = None,
    lastOutcome: Task.Outcome = Task.NotStarted,
    lastTriggeredTime: Option[DateTime] = None,
    lastScheduledTime: Option[DateTime] = None,
    lastExecutionTime: Option[DateTime] = None,
    finishedTime: Option[DateTime] = None
) {

  def finished: Boolean = finishedTime.isDefined

  def nextExecutionTime(implicit timeSource: TimeSource): Option[DateTime] = {
    import Trigger.{LastExecutionTime, ScheduledTime}

    val referenceTime = lastExecutionTime match {
      case Some(time) => LastExecutionTime(time)
      case None       => ScheduledTime(lastScheduledTime.getOrElse(createdTime))
    }
    trigger.nextExecutionTime(referenceTime)
  }

}
