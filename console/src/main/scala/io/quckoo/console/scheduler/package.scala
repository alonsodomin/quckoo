package io.quckoo.console

import io.quckoo.console.log._
import io.quckoo.protocol.Event
import io.quckoo.protocol.scheduler._

/**
  * Created by alonsodomin on 14/05/2017.
  */
package object scheduler {

  final val SchedulerLogger: Logger[Event] = {
    case ExecutionPlanStarted(jobId, planId, dateTime) =>
      LogRecord.info(dateTime, s"Started execution plan '$planId' for job '$jobId'.")

    case ExecutionPlanFinished(jobId, planId, dateTime) =>
      LogRecord.info(dateTime, s"Execution plan '$planId' for job '$jobId' has finished")

    case ExecutionPlanCancelled(jobId, planId, dateTime) =>
      LogRecord.error(dateTime, s"Execution plan '$planId' for job '$jobId' has been cancelled")

    case TaskScheduled(jobId, planId, task, dateTime) =>
      LogRecord.info(dateTime, s"Task ${task.id} for job '$jobId' in plan '$planId' has been scheduled.")

    case TaskTriggered(jobId, planId, taskId, dateTime) =>
      LogRecord.info(dateTime, s"Task $taskId for job '$jobId' in plan '$planId' has been triggered.")

    case TaskCompleted(jobId, planId, taskId, dateTime, _) =>
      LogRecord.info(dateTime, s"Task $taskId for job '$jobId' in plan '$planId' has completed.")
  }

}
