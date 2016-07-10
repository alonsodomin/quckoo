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

package io.quckoo.protocol.worker

import io.quckoo.id.NodeId
import io.quckoo.net.Location
import io.quckoo.protocol.Event

sealed trait WorkerEvent extends Event {
  val workerId: NodeId
}

final case class WorkerJoined(workerId: NodeId, location: Location) extends WorkerEvent
final case class WorkerLost(workerId: NodeId) extends WorkerEvent
final case class WorkerRemoved(workerId: NodeId) extends WorkerEvent
