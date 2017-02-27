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

package io.quckoo.net

import io.quckoo.NodeId

/**
  * Created by alonsodomin on 03/04/2016.
  */
sealed trait QuckooNode {
  val id: NodeId
  val location: Location
  def status: NodeStatus
}

final case class MasterNode(id: NodeId, location: Location, status: NodeStatus) extends QuckooNode
final case class WorkerNode(id: NodeId, location: Location, status: NodeStatus) extends QuckooNode
