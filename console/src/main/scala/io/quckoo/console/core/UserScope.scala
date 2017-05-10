/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.core

import diode.data._

import io.quckoo._

import monocle.macros.Lenses

@Lenses final case class UserScope(
    jobSpecs: PotMap[JobId, JobSpec],
    executionPlans: PotMap[PlanId, ExecutionPlan],
    executions: PotMap[TaskId, TaskExecution]
)

object UserScope {

  def initial = UserScope(
    jobSpecs = PotMap(JobSpecFetcher),
    executionPlans = PotMap(ExecutionPlanFetcher),
    executions = PotMap(ExecutionFetcher)
  )

}
