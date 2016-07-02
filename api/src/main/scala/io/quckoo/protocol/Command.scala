package io.quckoo.protocol

import diode.ActionType

/**
  * Created by alonsodomin on 02/07/2016.
  */
trait Command

object Command {
  implicit object actionType extends ActionType[Command]
}
