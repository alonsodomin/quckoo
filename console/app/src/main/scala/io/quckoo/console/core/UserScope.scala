package io.quckoo.console.core

import diode.data._
import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.id.{JobId, PlanId, TaskId}

final case class UserScope(
  jobSpecs: PotMap[JobId, JobSpec],
  executionPlans: PotMap[PlanId, ExecutionPlan],
  tasks: PotMap[TaskId, TaskItem]
)

object UserScope {

  def initial = UserScope(
    jobSpecs       = PotMap(JobSpecFetcher),
    executionPlans = PotMap(ExecutionPlanFetcher),
    tasks          = PotMap(TaskFetcher)
  )

}
