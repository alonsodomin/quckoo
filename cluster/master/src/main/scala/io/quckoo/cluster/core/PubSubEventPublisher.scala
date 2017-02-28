package io.quckoo.cluster.core

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source

import scala.reflect.ClassTag

/**
  * Created by alonsodomin on 28/02/2017.
  */

object PubSubEventPublisher {

  def source[A: ClassTag](topic: String)(implicit actorSystem: ActorSystem): Source[A, NotUsed] = {
    val publisherRef = actorSystem.actorOf(Props(new PubSubEventPublisher[A](topic)))
    Source.actorRef[A](50, OverflowStrategy.dropTail)
      .mapMaterializedValue { upstream =>

      }
    ???
  }

}

class PubSubEventPublisher[A: ClassTag](topic: String) extends Actor with ActorLogging {
  import DistributedPubSubMediator._

  private val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! Subscribe(topic, self)

  override def postStop(): Unit =
    mediator ! Unsubscribe(topic, self)

  override def receive: Receive = ???

  private def initializing: Receive = {
    case SubscribeAck(Subscribe(`topic`, _, `self`)) =>
  }

}
