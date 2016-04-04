package io.quckoo.protocol.worker

import io.quckoo.id.NodeId
import io.quckoo.net.Location

sealed trait WorkerEvent {
  val workerId: NodeId
}

final case class WorkerJoined(workerId: NodeId, location: Location) extends WorkerEvent
final case class WorkerRemoved(workerId: NodeId) extends WorkerEvent