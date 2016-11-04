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

package io.quckoo.worker.boot

import java.net.InetAddress
import java.util.{HashMap => JHashMap}

import com.typesafe.config.{Config, ConfigFactory}

import io.quckoo.worker.config._

import scala.collection.JavaConversions._

/**
  * Created by aalonsodominguez on 03/10/2015.
  */
object CliOptions {
  private final val QuckooContactPoints = "quckoo.worker.contact-points"
}

final case class CliOptions(
    bindAddress: Option[String] = None,
    port: Int = DefaultTcpPort,
    masterNodes: Seq[String] = Seq()
  ) {

  import CliOptions._

  def toConfig: Config = {
    val valueMap = new JHashMap[String, Object]()

    val (bindHost, bindPort) = bindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } getOrElse (DefaultTcpInterface -> port)

    valueMap.put(AkkaRemoteNettyHost, bindHost)
    valueMap.put(AkkaRemoteNettyPort, Int.box(bindPort))

    if (bindAddress.isDefined) {
      val localAddress = InetAddress.getLocalHost.getHostAddress
      valueMap.put(AkkaRemoteNettyBindHost, localAddress)
      valueMap.put(AkkaRemoteNettyBindPort, Int.box(port))
    }

    if (masterNodes.nonEmpty) {
      valueMap.put(QuckooContactPoints, seqAsJavaList(masterNodes.map { node =>
        s"akka.tcp://QuckooClusterSystem@$node"
      }))
    }
    ConfigFactory.parseMap(valueMap)
  }

}
