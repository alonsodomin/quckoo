package io.quckoo.net

import io.quckoo.id.NodeId
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker._
import monocle.macros.Lenses

/**
  * Created by alonsodomin on 03/04/2016.
  */
@Lenses final case class QuckooState(
    masterNodes: Map[NodeId, MasterNode] = Map.empty,
    workerNodes: Map[NodeId, WorkerNode] = Map.empty,
    metrics: QuckooMetrics = QuckooMetrics()
) {

  def updated(event: MasterEvent): QuckooState = event match {
    case MasterJoined(nodeId, location) =>
      copy(masterNodes = masterNodes + (nodeId -> MasterNode(nodeId, location, NodeStatus.Active)))

    case MasterRemoved(nodeId) =>
      copy(masterNodes = masterNodes - nodeId)

    case MasterUnreachable(nodeId) if masterNodes.contains(nodeId) =>
      val newNodeStatus = masterNodes(nodeId).copy(status = NodeStatus.Unreachable)
      copy(masterNodes = masterNodes + (nodeId -> newNodeStatus))

    case MasterReachable(nodeId) if masterNodes.contains(nodeId) =>
      val newNodeStatus = masterNodes(nodeId).copy(status = NodeStatus.Active)
      copy(masterNodes = masterNodes + (nodeId -> newNodeStatus))
  }

  def updated(event: WorkerEvent): QuckooState = event match {
    case WorkerJoined(workerId, location) =>
      copy(workerNodes = workerNodes + (workerId -> WorkerNode(workerId, location, NodeStatus.Active)))

    case WorkerRemoved(workerId) =>
      copy(workerNodes = workerNodes - workerId)
  }

}
