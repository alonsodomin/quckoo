package io.quckoo.client.internal

/**
  * Created by alonsodomin on 05/09/2016.
  */
sealed trait Protocol
object Protocol {
  sealed abstract class Http extends Protocol
  sealed abstract class Tcp extends Protocol
}
