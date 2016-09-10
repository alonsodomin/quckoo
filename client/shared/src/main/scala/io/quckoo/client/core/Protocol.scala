package io.quckoo.client.core

/**
  * Created by alonsodomin on 09/09/2016.
  */
sealed trait Protocol
object Protocol {
  sealed trait Http extends Protocol
}
