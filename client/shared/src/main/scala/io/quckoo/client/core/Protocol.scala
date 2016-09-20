package io.quckoo.client.core

/**
  * Created by alonsodomin on 17/09/2016.
  */
trait Protocol {
  type Request
  type Response
  type EventType
}
