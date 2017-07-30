/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.console

import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.log._
import io.quckoo.protocol.Event
import io.quckoo.protocol.cluster.{
  MasterJoined,
  MasterReachable,
  MasterRemoved,
  MasterUnreachable
}
import io.quckoo.protocol.worker.{WorkerJoined, WorkerLost, WorkerRemoved}

/**
  * Created by alonsodomin on 07/05/2017.
  */
package object dashboard {

  final val DashboardLogger: Logger[Event] = {
    case MasterJoined(_, location) =>
      LogRecord.info(s"Master node joined from: $location")

    case MasterUnreachable(nodeId) =>
      LogRecord.warning(s"Master node $nodeId has become unreachable")

    case MasterReachable(nodeId) =>
      LogRecord.info(s"Master node $nodeId has re-joined the cluster")

    case MasterRemoved(nodeId) =>
      LogRecord.warning(s"Master node $nodeId has left the cluster.")

    case WorkerJoined(_, location) =>
      LogRecord.info(s"Worker node joined from: $location")

    case WorkerLost(nodeId) =>
      LogRecord.warning(s"Worker $nodeId lost communication with the cluster")

    case WorkerRemoved(nodeId) =>
      LogRecord.error(s"Worker $nodeId has left the cluster")
  }

}
