package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.contrib.pattern.{ClusterReceptionistExtension, DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.protocol.ListenerProtocol._
import io.chronos.protocol._
import io.chronos.topic

/**
 * Created by aalonsodominguez on 13/07/15.
 */
object ExecutionMonitor {

}

class ExecutionMonitor extends Actor with ActorLogging {

  ClusterReceptionistExtension(context.system).registerService(self)
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
      context.become(ready, discardOld = false)
  }

  def ready: Receive = {
    case Subscribe =>
      subscribers += sender()
      sender() ! SubscribeAck

    case event: ExecutionEvent =>
      log.debug("Received execution event: " + event)
      subscribers.foreach { subscriber =>
        subscriber ! event
      }

    case Unsubscribe =>
      subscribers -= sender()
      sender() ! UnsubscribeAck

    case _: DistributedPubSubMediator.UnsubscribeAck =>
      subscribers = Set.empty
      context.unbecome()
  }

}
