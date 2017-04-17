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

import io.circe.generic.auto._

import io.quckoo.client.core.ChannelException
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization.json._

import monix.execution.Scheduler.Implicits.global

import slogging.LazyLogging

/**
  * Created by alonsodomin on 04/04/2016.
  */
private[core] trait ConsoleSubscriptions extends LazyLogging {

  val errorHandler: PartialFunction[Throwable, Unit] = {
    case ex: ChannelException =>
      logger.error("Topic '{}' has been unexpectedly closed.", ex.topicName)
  }

  def openSubscriptionChannels(implicit client: HttpQuckooClient): Unit = {
    client.channel[MasterEvent].subscribe(new ActionSubscriber[MasterEvent](errorHandler))
    client.channel[WorkerEvent].subscribe(new ActionSubscriber[WorkerEvent](errorHandler))
    client.channel[SchedulerEvent].subscribe(new ActionSubscriber[SchedulerEvent](errorHandler))
    client.channel[RegistryEvent].subscribe(new ActionSubscriber[RegistryEvent](errorHandler))
  }

}
