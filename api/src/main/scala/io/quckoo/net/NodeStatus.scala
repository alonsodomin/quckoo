package io.quckoo.net

/**
  * Created by alonsodomin on 03/04/2016.
  */
sealed trait NodeStatus

object NodeStatus {
  case object Active extends NodeStatus
  case object Unreachable extends NodeStatus
}
