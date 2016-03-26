package io.quckoo.protocol.cluster

case class NodeInfo(active: Int = 0, inactive: Int = 0)
case class ClusterInfo(nodeInfo: NodeInfo = NodeInfo(), workers: Int = 0)