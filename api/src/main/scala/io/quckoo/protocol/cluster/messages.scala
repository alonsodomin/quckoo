package io.quckoo.protocol.cluster

import io.quckoo.id.NodeId
import io.quckoo.net.Location

case object GetClusterStatus

sealed trait MasterEvent
case class MasterJoined(nodeId: NodeId, location: Location) extends MasterEvent
case class MasterReachable(nodeId: NodeId) extends MasterEvent
case class MasterUnreachable(nodeId: NodeId) extends MasterEvent
case class MasterRemoved(nodeId: NodeId) extends MasterEvent
