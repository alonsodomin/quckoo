package io.quckoo.client

import io.quckoo.api.{Auth, Registry, Scheduler}

/**
  * Created by alonsodomin on 26/03/2016.
  */
trait QuckooClient extends Registry with Scheduler with Auth {

}
