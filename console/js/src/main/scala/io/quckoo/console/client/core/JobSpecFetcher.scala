package io.quckoo.console.client.core

import diode.data.Fetch
import io.quckoo.id.JobId

/**
  * Created by alonsodomin on 14/03/2016.
  */
object JobSpecFetcher extends Fetch[JobId] {

  override def fetch(key: JobId): Unit =
    ConsoleCircuit.dispatch(RefreshJobSpecs(keys = Set(key)))

  override def fetch(keys: Traversable[JobId]): Unit =
    ConsoleCircuit.dispatch(RefreshJobSpecs(keys.toSet))

}
