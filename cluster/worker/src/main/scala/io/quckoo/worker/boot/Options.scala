package io.quckoo.worker.boot

import java.net.InetAddress
import java.util.{HashMap => JHashMap, Map => JMap}

import com.typesafe.config.{Config, ConfigFactory}
import io.quckoo.worker.QuckooWorkerSettings

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 03/10/2015.
 */
object Options {

  final val SystemName = "QuckooWorkerSystem"

  final val DefaultPort = 5001

  final val AkkaRemoteNettyHost = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort = "akka.remote.netty.tcp.port"
  final val AkkaRemoteNettyBindHost = "akka.remote.netty.tcp.bind-hostname"
  final val AkkaRemoteNettyBindPort = "akka.remote.netty.tcp.bind-port"

  final val QuckooContactPoints = "quckoo.worker.contact-points"

  private final val HostAndPort = """(.+?):(\d+)""".r

}

case class Options(bindAddress: Option[String] = None,
                   port: Int = QuckooWorkerSettings.DefaultTcpPort,
                   masterNodes: Seq[String] = Seq()) {
  import Options._

  def toConfig: Config = {
    val valueMap = new JHashMap[String, Object]()

    val (bindHost, bindPort) = bindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } getOrElse((QuckooWorkerSettings.DefaultTcpInterface, port))

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
