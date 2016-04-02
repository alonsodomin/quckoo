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
