package io.chronos.worker.boot

import java.util.{HashMap => JHashMap, Map => JMap}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 03/10/2015.
 */
object Options {

  final val AkkaRemoteNettyHost = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort = "akka.remote.netty.tcp.port"

  final val ChronosContactPoints = "chronos.contact-points"

}

case class Options(host: String = "127.0.0.1", port: Int = 0, masterNodes: Seq[String] = Seq()) {
  import Options._

  def asJavaMap: JMap[String, Object] = {
    val map = new JHashMap[String, Object]()
    map.put(AkkaRemoteNettyHost, host)
    map.put(AkkaRemoteNettyPort, Int.box(port))
    if (masterNodes.nonEmpty) {
      map.put(ChronosContactPoints, seqAsJavaList(masterNodes.map { node =>
        s"akka.tcp://ChronosClusterSystem@$node"
      }))
    }
    map
  }

}
