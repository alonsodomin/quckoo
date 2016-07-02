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

package io.quckoo.protocol.cluster

import io.quckoo.id.NodeId
import io.quckoo.net.Location
import io.quckoo.protocol.{Command, Event}

case object GetClusterStatus extends Command

sealed trait MasterEvent extends Event

final case class MasterJoined(nodeId: NodeId, location: Location) extends MasterEvent
final case class MasterReachable(nodeId: NodeId) extends MasterEvent
final case class MasterUnreachable(nodeId: NodeId) extends MasterEvent
final case class MasterRemoved(nodeId: NodeId) extends MasterEvent
