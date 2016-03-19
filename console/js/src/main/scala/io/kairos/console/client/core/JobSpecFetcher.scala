package io.kairos.console.client.core

import diode.data.Fetch
import io.kairos.id.JobId

/**
  * Created by alonsodomin on 14/03/2016.
  */
object JobSpecFetcher extends Fetch[JobId] {

  override def fetch(key: JobId): Unit =
    KairosCircuit.dispatch(RefreshJobSpecs(keys = Set(key)))

  override def fetch(keys: Traversable[JobId]): Unit =
    KairosCircuit.dispatch(RefreshJobSpecs(keys.toSet))

}
