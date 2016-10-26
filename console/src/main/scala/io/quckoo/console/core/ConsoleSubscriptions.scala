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

package io.quckoo.console.core

import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization.json._

import monix.execution.Scheduler.Implicits.global

/**
  * Created by alonsodomin on 04/04/2016.
  */
private[core] trait ConsoleSubscriptions {

  def subscribeClusterState(implicit client: HttpQuckooClient): Unit = {
    client.channel[MasterEvent].subscribe(new SimpleEventSubscriber[MasterEvent])
    client.channel[WorkerEvent].subscribe(new SimpleEventSubscriber[WorkerEvent])
    client.channel[SchedulerEvent].subscribe(new SimpleEventSubscriber[SchedulerEvent])
    client.channel[RegistryEvent].subscribe(new SimpleEventSubscriber[RegistryEvent])
  }

}
