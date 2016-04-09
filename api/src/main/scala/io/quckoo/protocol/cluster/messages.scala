package io.quckoo.protocol.cluster

import io.quckoo.id.NodeId
import io.quckoo.net.Location

case object GetClusterStatus

sealed trait MasterEvent

final case class MasterJoined(nodeId: NodeId, location: Location) extends MasterEvent
final case class MasterReachable(nodeId: NodeId) extends MasterEvent
final case class MasterUnreachable(nodeId: NodeId) extends MasterEvent
final case class MasterRemoved(nodeId: NodeId) extends MasterEvent
