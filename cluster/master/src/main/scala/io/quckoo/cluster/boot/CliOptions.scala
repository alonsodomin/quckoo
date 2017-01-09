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

import io.quckoo.cluster.SystemName
import io.quckoo.cluster.config._

import scala.collection.JavaConversions._

/**
  * Created by aalonsodominguez on 03/10/2015.
  */
final case class CliOptions(
    bindAddress: Option[String] = None,
    port: Int = DefaultUdpPort,
    httpBindAddress: Option[String] = None,
    httpPort: Option[Int] = None,
    seed: Boolean = false,
    seedNodes: Seq[String] = Seq(),
    cassandraSeedNodes: Seq[String] = Seq()
) {

  def toConfig: Config = {
    val valueMap = new JHashMap[String, Object]()

    val (canonicalHost, canonicalPort) = bindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } getOrElse (DefaultUdpInterface -> port)

    valueMap.put(AkkaArteryCanonicalHost, canonicalHost)
    valueMap.put(AkkaArteryCanonicalPort, Int.box(canonicalPort))

    if (bindAddress.isDefined) {
      val localAddress = InetAddress.getLocalHost.getHostAddress
      valueMap.put(AkkaArteryBindHost, localAddress)
      valueMap.put(AkkaArteryBindPort, Int.box(port))
    }

    httpBindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } orElse httpPort.map(p => ("0.0.0.0", p)) foreach {
      case (intf, p) =>
        valueMap.put(QuckooHttpBindInterface, intf)
        valueMap.put(QuckooHttpBindPort, Int.box(p))
    }

    val clusterSeedNodes: List[String] = {
      val baseSeed = {
        if (seed) List(s"akka://$SystemName@$canonicalHost:$canonicalPort")
        else List.empty[String]
      }

      baseSeed ::: seedNodes.map(node => s"akka://$SystemName@$node").toList
    }

    valueMap.put(AkkaClusterSeedNodes, seqAsJavaList(clusterSeedNodes))

    if (cassandraSeedNodes.nonEmpty) {
      valueMap.put(CassandraJournalContactPoints, seqAsJavaList(cassandraSeedNodes))
      valueMap.put(CassandraSnapshotContactPoints, seqAsJavaList(cassandraSeedNodes))
    }

    ConfigFactory.parseMap(valueMap)
  }

}
