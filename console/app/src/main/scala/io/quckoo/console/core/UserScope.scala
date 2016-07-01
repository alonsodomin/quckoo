package io.quckoo.console.core

import diode.data._

import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.protocol.scheduler.TaskDetails

final case class UserScope(
  jobSpecs: PotMap[JobId, JobSpec],
  executionPlans: PotMap[PlanId, ExecutionPlan],
  tasks: PotMap[TaskId, TaskDetails]
)

object UserScope {

  def initial = UserScope(
    jobSpecs       = PotMap(JobSpecFetcher),
    executionPlans = PotMap(ExecutionPlanFetcher),
    tasks          = PotMap(TaskFetcher)
  )

}
