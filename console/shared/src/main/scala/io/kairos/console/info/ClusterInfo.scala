package io.kairos.console.info

/**
  * Created by alonsodomin on 15/12/2015.
  */
case class NodeInfo(active: Int = 0, inactive: Int = 0)

case class ClusterInfo(nodeInfo: NodeInfo = NodeInfo(), workers: Int = 0)
