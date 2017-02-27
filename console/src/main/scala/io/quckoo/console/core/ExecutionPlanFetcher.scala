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

import diode.data.Fetch

import io.quckoo.PlanId

/**
  * Created by alonsodomin on 14/03/2016.
  */
object ExecutionPlanFetcher extends Fetch[PlanId] {
  override def fetch(key: PlanId): Unit =
    ConsoleCircuit.dispatch(RefreshExecutionPlans(keys = Set(key)))

  override def fetch(keys: Traversable[PlanId]): Unit =
    ConsoleCircuit.dispatch(RefreshExecutionPlans(keys.toSet))
}
