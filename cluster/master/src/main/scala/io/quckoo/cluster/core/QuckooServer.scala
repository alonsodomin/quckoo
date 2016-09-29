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

package io.quckoo.cluster.core

import akka.NotUsed
import akka.stream.scaladsl.Source

import io.quckoo.api.{Cluster, EventDef, Registry, Scheduler}
import io.quckoo.cluster.registry.RegistryStreams
import io.quckoo.cluster.scheduler.SchedulerStreams
import io.quckoo.protocol.Event

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait QuckooServer extends Auth
    with Cluster with ClusterStreams
    with Registry with RegistryStreams
    with Scheduler with SchedulerStreams {

  def events: Source[(Event, EventDef[_ <: Event]), NotUsed]

}
