package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.protocol.ListenerProtocol._
import io.chronos.protocol.SchedulerProtocol.ExecutionEvent
import io.chronos.topic

/**
 * Created by aalonsodominguez on 13/07/15.
 */
object ExecutionMonitor {

}

class ExecutionMonitor extends Actor with ActorLogging {

  private val mediator = DistributedPubSubExtension(context.system).mediator

  var subscribers = Set.empty[ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    mediator ! DistributedPubSubMediator.Subscribe(topic.Executions, self)
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(topic.Executions, self)
  }

  def receive = {
    case DistributedPubSubMediator.SubscribeAck(subscribe) =>
      log.info("Subscribed to topic: " + subscribe.topic)
      context.become(ready, discardOld = false)
  }

  def ready: Receive = {
    case Subscribe(subscriber) =>
      subscribers += subscriber
      subscriber ! SubscribeAck

    case event: ExecutionEvent =>
      log.info("Received execution event: " + event)
      subscribers.foreach { subscriber =>
        subscriber ! event
      }

    case Unsubscribe(subscriber) =>
      subscribers -= subscriber
      subscriber ! UnsubscribeAck

    case _: DistributedPubSubMediator.UnsubscribeAck =>
      subscribers = Set.empty
      context.unbecome()
  }

}
