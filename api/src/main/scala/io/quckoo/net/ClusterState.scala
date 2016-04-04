package io.quckoo.net

import io.quckoo.id.NodeId
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker._

/**
  * Created by alonsodomin on 03/04/2016.
  */
final case class ClusterState(
    masterNodes: Map[NodeId, MasterNode] = Map.empty,
    workerNodes: Map[NodeId, WorkerNode] = Map.empty
) {

  def updated(event: MasterEvent): ClusterState = event match {
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

  def updated(event: WorkerEvent): ClusterState = event match {
    case WorkerJoined(workerId, location) =>
      copy(workerNodes = workerNodes + (workerId -> WorkerNode(workerId, location, NodeStatus.Active)))

    case WorkerRemoved(workerId) =>
      copy(workerNodes = workerNodes - workerId)
  }

}
