package io.kairos.console.client.core

import diode.data.Fetch
import io.kairos.id.PlanId

/**
  * Created by alonsodomin on 14/03/2016.
  */
object ScheduleFetcher extends Fetch[PlanId] {
  override def fetch(key: PlanId): Unit =
    KairosCircuit.dispatch(UpdateSchedules(keys = Set(key)))

  override def fetch(keys: Traversable[PlanId]): Unit =
    KairosCircuit.dispatch(UpdateSchedules(keys.toSet))
}
