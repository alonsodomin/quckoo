package io.quckoo.cluster.registry

import akka.actor.Props
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import io.quckoo.cluster.core.EventPublisher
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.topics

/**
  * Created by alonsodomin on 28/03/2016.
  */
object RegistryEventPublisher {

  def props: Props = Props(classOf[RegistryEventPublisher])

}
class RegistryEventPublisher extends EventPublisher[RegistryEvent] {

  private val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(topics.RegistryTopic, self)

  override def receive: Receive = initializing
  
  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      context.become(emitEvents)
  }

  def emitEvents: Receive = {
    case event: RegistryEvent =>
      emitEvent(event)
  }

}
