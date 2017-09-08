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

package io.quckoo.console.dashboard

import diode.{Effect, ModelRW}

import io.quckoo.console.components.Notification
import io.quckoo.console.core._
import io.quckoo.net.QuckooState
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.scheduler.TaskQueueUpdated
import io.quckoo.protocol.worker._

import slogging._

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 14/05/2017.
  */
class DashboardHandler(model: ModelRW[ConsoleScope, QuckooState],
                       ops: ConsoleOps)(implicit ec: ExecutionContext)
    extends ConsoleHandler[QuckooState](model) with AuthHandler[QuckooState] with LazyLogging {

  override def handle = {
    case GetClusterStatus =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.refreshClusterStatus))
      }

    case ClusterStateLoaded(state) =>
      updated(state)

    case evt: MasterEvent =>
      val notification = evt match {
        case MasterJoined(_, location) =>
          Notification.success(s"Master node joined from: $location")

        case MasterUnreachable(nodeId) =>
          Notification.warning(s"Master node $nodeId has become unreachable")

        case MasterReachable(nodeId) =>
          Notification.info(s"Master node $nodeId has re-joined the cluster")

        case MasterRemoved(nodeId) =>
          Notification.danger(s"Master node $nodeId has left the cluster.")
      }
      updated(value.updated(evt), Growl(notification))

    case evt: WorkerEvent =>
      val notification = evt match {
        case WorkerJoined(_, location) =>
          Notification.success(s"Worker node joined from: $location")

        case WorkerLost(nodeId) =>
          Notification.warning(s"Worker $nodeId lost communication with the cluster")

        case WorkerRemoved(nodeId) =>
          Notification.danger(s"Worker $nodeId has left the cluster")
      }
      updated(value.updated(evt), Growl(notification))

    case evt: TaskQueueUpdated =>
      updated(value.copy(metrics = value.metrics.updated(evt)))
  }

}
