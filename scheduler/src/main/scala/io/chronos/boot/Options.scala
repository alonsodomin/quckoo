package io.chronos.boot

import java.net.InetAddress
import java.util.{HashMap => JHashMap, Map => JMap}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 03/10/2015.
 */
object Options {

  final val DefaultPort = 2551

  final val AkkaRemoteNettyHost = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort = "akka.remote.netty.tcp.port"
  final val AkkaRemoteNettyBindHost = "akka.remote.netty.tcp.bind-hostname"
  final val AkkaRemoteNettyBindPort = "akka.remote.netty.tcp.bind-port"

  final val AkkaClusterSeedNodes = "akka.cluster.seed-nodes"

  final val ExternalHostname = "chronos.network.external.hostname"
  final val ExternalPort = "chronos.network.external.port"

  final val CassandraJournalContactPoints = "cassandra-journal.contact-points"
  final val CassandraSnapshotContactPoints = "cassandra-snapshot-store.contact-points"

  private final val HostAndPort = """(.+?):(\d+)""".r

}

case class Options(bindAddress: String = s"localhost:${Options.DefaultPort}",
                   port: Int = Options.DefaultPort,
                   seed: Boolean = false,
                   seedNodes: Seq[String] = Seq(),
                   cassandraSeedNodes: Seq[String] = Seq()) {
  import Options._

  def asJavaMap: JMap[String, Object] = {
    val map = new JHashMap[String, Object]()

    val HostAndPort(externalHost, externalPort) = bindAddress
    map.put(AkkaRemoteNettyHost, externalHost)
    map.put(AkkaRemoteNettyPort, externalPort)

    val localAddress = InetAddress.getLocalHost.getHostAddress
    map.put(AkkaRemoteNettyBindHost, localAddress)
    map.put(AkkaRemoteNettyBindPort, Int.box(port))

    val clusterSeedNodes: Seq[String] = {
      if (seed || seedNodes.isEmpty)
        List(s"akka.tcp://ChronosClusterSystem@$externalHost:$externalPort")
      else
        List.empty[String]
    } ::: seedNodes.map({ node =>
      s"akka.tcp://ChronosClusterSystem@$node"
    }).toList

    map.put(AkkaClusterSeedNodes, seqAsJavaList(clusterSeedNodes))

    if (cassandraSeedNodes.nonEmpty) {
      map.put(CassandraJournalContactPoints, seqAsJavaList(cassandraSeedNodes))
      map.put(CassandraSnapshotContactPoints, seqAsJavaList(cassandraSeedNodes))
    }
    map
  }

}
