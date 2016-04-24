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

import akka.actor.ActorLogging
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import scala.reflect.ClassTag

/**
  * Created by alonsodomin on 01/04/2016.
  */
abstract class PubSubSubscribedEventPublisher[A: ClassTag](topic: String)
    extends EventPublisher[A] with ActorLogging {

  private val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(topic, self)

  override def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      log.info("Event stream publisher for topic '{}' initialized.", topic)
      context.become(emitEvents)
  }

  def emitEvents: Receive = {
    case event: A =>
      log.debug("Publishing event: {}", event)
      emitEvent(event)
  }

}
