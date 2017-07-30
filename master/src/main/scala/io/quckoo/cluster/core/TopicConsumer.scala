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

package io.quckoo.cluster.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import io.quckoo.api.TopicTag

/**
  * Created by alonsodomin on 01/03/2017.
  */
object TopicConsumer {
  case object Consume
}

object LocalTopicConsumer {

  def props[A](implicit topicTag: TopicTag[A]): Props =
    Props(
      new LocalTopicConsumer(topicTag.name,
                             Seq(topicTag.eventType.runtimeClass)))

}

class LocalTopicConsumer private (topicName: String, classes: Seq[Class[_]])
    extends Actor
    with ActorLogging {
  import TopicConsumer._

  log.debug("Preparing to consume topic '{}'.", topicName)

  override def preStart(): Unit = {
    classes.foreach { clazz =>
      context.system.eventStream.subscribe(self, clazz)
    }
  }

  override def postStop(): Unit =
    context.system.eventStream.unsubscribe(self)

  override def receive: Receive = {
    case Consume =>
      log.debug("Starting to publish events from topic '{}' into the stream.",
                topicName)
      context.become(sendTo(sender()))
  }

  private def sendTo(target: ActorRef): Receive = {
    case msg => target ! msg
  }

}

object PubSubTopicConsumer {

  def props[A](implicit topicTag: TopicTag[A]): Props = {
    Props(new PubSubTopicConsumer(topicTag.name))
  }

}

class PubSubTopicConsumer private (topic: String)
    extends Actor
    with ActorLogging
    with Stash {
  import DistributedPubSubMediator._
  import TopicConsumer._

  private val mediator = DistributedPubSub(context.system).mediator
  log.debug("Preparing to read topic '{}'.", topic)

  override def preStart(): Unit =
    mediator ! Subscribe(topic, self)

  override def postStop(): Unit =
    mediator ! Unsubscribe(topic, self)

  override def receive: Receive = initializing()

  private def initializing(subscribed: Boolean = false,
                           target: Option[ActorRef] = None): Receive = {
    case SubscribeAck(Subscribe(`topic`, _, `self`)) =>
      val nextBehaviour = target
        .map(switchToRunning)
        .getOrElse(initializing(subscribed = true))
      context.become(nextBehaviour)

    case Consume if subscribed =>
      context.become(switchToRunning(sender()))

    case Consume =>
      context.become(initializing(target = Some(sender())))

    case _ => stash()
  }

  private def running(target: ActorRef): Receive = {
    case msg => target ! msg
  }

  private[this] def switchToRunning(targetRef: ActorRef): Receive = {
    log.debug("Starting to publish events from topic '{}' into the stream.",
              topic)
    unstashAll()
    running(targetRef)
  }

}
