package io.kairos.console.model

import io.kairos.Trigger
import io.kairos.id.{PlanId, JobId}
import io.kairos.time.DateTime

/**
  * Created by alonsodomin on 14/03/2016.
  */
final case class ExecutionPlanDetails(
    jobId: JobId,
    planId: PlanId,
    trigger: Trigger,
    lastExecutionTime: Option[DateTime]
)
