package io.kairos.console.client.registry

import diode.data.Fetch
import io.kairos.id.JobId

/**
  * Created by alonsodomin on 21/02/2016.
  */
class JobSpecFetcher extends Fetch[JobId] {

  override def fetch(key: JobId): Unit = ???

  override def fetch(keys: Traversable[JobId]): Unit = ???

}
