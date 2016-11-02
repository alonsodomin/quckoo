/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.cluster.boot

import java.net.InetAddress
import java.util.{HashMap => JHashMap}

import com.typesafe.config.{Config, ConfigFactory}
import io.quckoo.cluster.QuckooClusterSettings

import scala.collection.JavaConversions._

/**
  * Created by aalonsodominguez on 03/10/2015.
  */
object Options {

  final val SystemName = "QuckooClusterSystem"

  final val AkkaRemoteNettyHost     = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort     = "akka.remote.netty.tcp.port"
  final val AkkaRemoteNettyBindHost = "akka.remote.netty.tcp.bind-hostname"
  final val AkkaRemoteNettyBindPort = "akka.remote.netty.tcp.bind-port"

  final val AkkaClusterSeedNodes = "akka.cluster.seed-nodes"

  final val CassandraJournalContactPoints  = "cassandra-journal.contact-points"
  final val CassandraSnapshotContactPoints = "cassandra-snapshot-store.contact-points"

  final val QuckooHttpBindInterface = "quckoo.http.bind-interface"
  final val QuckooHttpBindPort      = "quckoo.http.bind-port"

  private final val HostAndPort = """(.+?):(\d+)""".r

}

case class Options(
    bindAddress: Option[String] = None,
    port: Int = QuckooClusterSettings.DefaultTcpPort,
    httpBindAddress: Option[String] = None,
    httpPort: Option[Int] = None,
    seed: Boolean = false,
    seedNodes: Seq[String] = Seq(),
    cassandraSeedNodes: Seq[String] = Seq()
) {
  import Options._

  def toConfig: Config = {
    val valueMap = new JHashMap[String, Object]()

    val (bindHost, bindPort) = bindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } getOrElse (QuckooClusterSettings.DefaultTcpInterface -> port)

    valueMap.put(AkkaRemoteNettyHost, bindHost)
    valueMap.put(AkkaRemoteNettyPort, Int.box(bindPort))

    if (bindAddress.isDefined) {
      val localAddress = InetAddress.getLocalHost.getHostAddress
      valueMap.put(AkkaRemoteNettyBindHost, localAddress)
      valueMap.put(AkkaRemoteNettyBindPort, Int.box(port))
    }

    httpBindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } orElse httpPort.map(p => ("0.0.0.0", p)) foreach {
      case (intf, p) =>
        valueMap.put(QuckooHttpBindInterface, intf)
        valueMap.put(QuckooHttpBindPort, Int.box(p))
    }

    val clusterSeedNodes: Seq[String] = {
      if (seed || seedNodes.isEmpty)
        List(s"akka.tcp://$SystemName@$bindHost:$bindPort")
      else
        List.empty[String]
    } ::: seedNodes
      .map({ node =>
        s"akka.tcp://$SystemName@$node"
      })
      .toList

    valueMap.put(AkkaClusterSeedNodes, seqAsJavaList(clusterSeedNodes))

    if (cassandraSeedNodes.nonEmpty) {
      valueMap.put(CassandraJournalContactPoints, seqAsJavaList(cassandraSeedNodes))
      valueMap.put(CassandraSnapshotContactPoints, seqAsJavaList(cassandraSeedNodes))
    }

    ConfigFactory.parseMap(valueMap)
  }

}
