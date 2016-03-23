package io.quckoo.examples

import java.util.{HashMap => JHashMap, Map => JMap}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 04/10/2015.
 */
object CliOptions {

  final val KairosContactPoints = "kairos.contact-points"

}

case class CliOptions(clusterNodes: Seq[String] = Seq()) {
  import CliOptions._

  def asJavaMap: JMap[String, Object] = {
    val map = new JHashMap[String, Object]()
    map.put(KairosContactPoints, seqAsJavaList(clusterNodes.map { node =>
      s"akka.tcp://KairosClusterSystem@$node"
    }))
    map
  }

}
