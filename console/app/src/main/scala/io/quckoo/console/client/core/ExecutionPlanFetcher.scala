package io.quckoo.console.client.core

import diode.data.Fetch
import io.quckoo.id.PlanId

/**
  * Created by alonsodomin on 14/03/2016.
  */
object ExecutionPlanFetcher extends Fetch[PlanId] {
  override def fetch(key: PlanId): Unit =
    ConsoleCircuit.dispatch(RefreshExecutionPlans(keys = Set(key)))

  override def fetch(keys: Traversable[PlanId]): Unit =
    ConsoleCircuit.dispatch(RefreshExecutionPlans(keys.toSet))
}
