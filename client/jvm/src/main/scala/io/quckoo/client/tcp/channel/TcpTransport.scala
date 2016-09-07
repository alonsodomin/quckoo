package io.quckoo.client.tcp.channel

import akka.actor.ActorSystem
import akka.cluster.client.ClusterClientSettings
import io.quckoo.client.internal.{Protocol, Transport}
import io.quckoo.client.tcp.QuckooTcpClient

/**
  * Created by alonsodomin on 06/09/2016.
  */
class TcpTransport(clusterClientSettings: ClusterClientSettings, maxConnectionAttempts: Int = 3)
                  (implicit actorSystem: ActorSystem)
  extends Transport[Protocol.Tcp] {

  val client = actorSystem.actorOf(QuckooTcpClient.props(clusterClientSettings, maxConnectionAttempts), "client")

}
