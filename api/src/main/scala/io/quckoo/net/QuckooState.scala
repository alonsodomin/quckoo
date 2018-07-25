/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.net

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

import io.quckoo.NodeId
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
      copy(
        workerNodes = workerNodes + (workerId -> WorkerNode(
            workerId,
            location,
            NodeStatus.Active)))

    case WorkerLost(workerId) =>
      val currentState = workerNodes.get(workerId)
      currentState.map { node =>
        copy(workerNodes = workerNodes + (workerId -> node.copy(status = NodeStatus.Unreachable)))
      } getOrElse this

    case WorkerRemoved(workerId) =>
      copy(workerNodes = workerNodes - workerId)
  }

}
object QuckooState {
  implicit val quckooStateEncoder: Encoder[QuckooState] = deriveEncoder[QuckooState]
  implicit val quckooStateDecoder: Decoder[QuckooState] = deriveDecoder[QuckooState]
}
