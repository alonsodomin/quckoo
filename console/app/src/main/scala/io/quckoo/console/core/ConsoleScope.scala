/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import io.quckoo.client.QuckooClient
import io.quckoo.client.ajax.AjaxQuckooClientFactory
import io.quckoo.console.components.Notification
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.net.QuckooState
import io.quckoo.{ExecutionPlan, JobSpec}

/**
  * Created by alonsodomin on 20/02/2016.
  */

case class ConsoleScope private (
                                  client: Option[QuckooClient],
                                  notification: Option[Notification],
                                  clusterState: QuckooState,
                                  jobSpecs: PotMap[JobId, JobSpec],
                                  executionPlans: PotMap[PlanId, ExecutionPlan]
) {

  def currentUser = client.flatMap(_.principal)

}

object ConsoleScope {

  def initial =
    ConsoleScope(
      client         = AjaxQuckooClientFactory.autoConnect(),
      notification   = None,
      clusterState   = QuckooState(),
      jobSpecs       = PotMap(JobSpecFetcher),
      executionPlans = PotMap(ExecutionPlanFetcher)
    )

}
