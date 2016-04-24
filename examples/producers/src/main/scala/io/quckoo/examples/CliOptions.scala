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
