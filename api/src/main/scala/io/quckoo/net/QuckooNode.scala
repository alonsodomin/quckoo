package io.quckoo.net

import io.quckoo.id.NodeId

/**
  * Created by alonsodomin on 03/04/2016.
  */
sealed trait QuckooNode {
  val id: NodeId
  val location: Location
  def status: NodeStatus
}

final case class MasterNode(id: NodeId, location: Location, status: NodeStatus) extends QuckooNode
final case class WorkerNode(id: NodeId, location: Location, status: NodeStatus) extends QuckooNode