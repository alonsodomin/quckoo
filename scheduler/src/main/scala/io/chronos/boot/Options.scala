package io.chronos.boot

import java.util.{HashMap => JHashMap, Map => JMap}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 03/10/2015.
 */
object Options {

  final val DefaultPort = 2551

  final val AkkaRemoteNettyHost = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort = "akka.remote.netty.tcp.port"

  final val AkkaClusterSeedNodes = "akka.cluster.seed-nodes"

  final val CassandraJournalContactPoints = "cassandra-journal.contact-points"
  final val CassandraSnapshotContactPoints = "cassandra-snapshot-store.contact-points"

}

case class Options(host: String = "127.0.0.1",
                   port: Int = Options.DefaultPort,
                   seedNodes: Seq[String] = Seq(),
                   cassandraSeedNodes: Seq[String] = Seq()) {
  import Options._

  def asJavaMap: JMap[String, Object] = {
    val map = new JHashMap[String, Object]()
    map.put(AkkaRemoteNettyHost, host)
    map.put(AkkaRemoteNettyPort, Int.box(port))

    if (seedNodes.nonEmpty) {
      map.put(AkkaClusterSeedNodes, seqAsJavaList(seedNodes.map { node =>
        s"akka.tcp://ChronosClusterSystem@$node"
      }))
    }
    if (cassandraSeedNodes.nonEmpty) {
      map.put(CassandraJournalContactPoints, seqAsJavaList(cassandraSeedNodes))
      map.put(CassandraSnapshotContactPoints, seqAsJavaList(cassandraSeedNodes))
    }
    map
  }

}
