package io.quckoo.client.http

import io.quckoo.client.core._
import io.quckoo.net.QuckooState

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait HttpClusterCmds extends HttpMarshalling with ClusterCmds[HttpProtocol] {
  import CmdMarshalling.Auth

  implicit lazy val getClusterStateCmd: GetClusterStateCmd = new Auth[HttpProtocol, Unit, QuckooState] {
    override val marshall = marshallEmpty[GetClusterStateCmd](HttpMethod.Get, _ => ClusterStateURI)
    override val unmarshall = unmarshallFromJson[GetClusterStateCmd]
  }

}
