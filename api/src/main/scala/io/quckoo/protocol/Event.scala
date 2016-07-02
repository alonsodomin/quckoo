package io.quckoo.protocol

import diode.ActionType

/**
  * Created by alonsodomin on 02/07/2016.
  */
trait Event

object Event {
  implicit object eventType extends ActionType[Event]
}
