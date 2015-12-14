package io.kairos.worker.boot

import java.net.InetAddress
import java.util.{HashMap => JHashMap, Map => JMap}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 03/10/2015.
 */
object Options {

  final val DefaultPort = 5001

  final val AkkaRemoteNettyHost = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort = "akka.remote.netty.tcp.port"
  final val AkkaRemoteNettyBindHost = "akka.remote.netty.tcp.bind-hostname"
  final val AkkaRemoteNettyBindPort = "akka.remote.netty.tcp.bind-port"

  final val KairosContactPoints = "kairos.contact-points"

  private final val HostAndPort = """(.+?):(\d+)""".r

}

case class Options(bindAddress: String = s"localhost:${Options.DefaultPort}",
                   port: Int = Options.DefaultPort,
                   masterNodes: Seq[String] = Seq()) {
  import Options._

  def asJavaMap: JMap[String, Object] = {
    val map = new JHashMap[String, Object]()

    val HostAndPort(externalHost, externalPort) = bindAddress
    map.put(AkkaRemoteNettyHost, externalHost)
    map.put(AkkaRemoteNettyPort, externalPort)

    val localAddress = InetAddress.getLocalHost.getHostAddress
    map.put(AkkaRemoteNettyBindHost, localAddress)
    map.put(AkkaRemoteNettyBindPort, Int.box(port))

    if (masterNodes.nonEmpty) {
      map.put(KairosContactPoints, seqAsJavaList(masterNodes.map { node =>
        s"akka.tcp://KairosClusterSystem@$node"
      }))
    }
    map
  }

}
