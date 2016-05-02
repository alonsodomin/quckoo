package io.quckoo.console.core

import diode.data._

import io.quckoo.{JobSpec, ExecutionPlan}
import io.quckoo.id.{JobId, PlanId}

final case class UserScope(
  jobSpecs: PotMap[JobId, JobSpec],
  executionPlans: PotMap[PlanId, ExecutionPlan]
)

object UserScope {

  def initial = UserScope(
    jobSpecs       = PotMap(JobSpecFetcher),
    executionPlans = PotMap(ExecutionPlanFetcher)
  )

}
