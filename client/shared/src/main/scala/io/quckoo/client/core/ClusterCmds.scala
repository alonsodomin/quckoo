package io.quckoo.client.core

import io.quckoo.net.QuckooState

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait ClusterCmds[P <: Protocol] {
  import CmdMarshalling.Auth

  type GetClusterStateCmd = Auth[P, Unit, QuckooState]

  implicit def getClusterStateCmd: GetClusterStateCmd
}
