package io.kairos.examples

import java.util.{HashMap => JHashMap, Map => JMap}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 04/10/2015.
 */
object CliOptions {

  final val ChronosContactPoints = "chronos.contact-points"

}

case class CliOptions(clusterNodes: Seq[String] = Seq()) {
  import CliOptions._

  def asJavaMap: JMap[String, Object] = {
    val map = new JHashMap[String, Object]()
    map.put(ChronosContactPoints, seqAsJavaList(clusterNodes.map { node =>
      s"akka.tcp://ChronosClusterSystem@$node"
    }))
    map
  }

}
