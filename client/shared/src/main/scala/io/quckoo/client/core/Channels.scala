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

package io.quckoo.client.core

import io.quckoo.api.TopicTag
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization.Decoder

/**
  * Created by domingueza on 20/09/2016.
  */
trait Channels[P <: Protocol] {

  type MasterChannel    = Channel.Aux[P, MasterEvent]
  type WorkerChannel    = Channel.Aux[P, WorkerEvent]
  type RegistryChannel  = Channel.Aux[P, RegistryEvent]
  type SchedulerChannel = Channel.Aux[P, SchedulerEvent]

  def createChannel[E: TopicTag](implicit decoder: Decoder[String, E]): Channel.Aux[P, E]

}
