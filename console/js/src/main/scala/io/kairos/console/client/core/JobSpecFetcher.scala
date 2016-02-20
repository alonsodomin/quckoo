package io.kairos.console.client.core

import diode.data.Fetch
import io.kairos.id.JobId

/**
  * Created by alonsodomin on 20/02/2016.
  */
object JobSpecFetcher extends Fetch[JobId] {
  override def fetch(key: JobId): Unit = ???

  override def fetch(keys: Traversable[JobId]): Unit = ???
}
