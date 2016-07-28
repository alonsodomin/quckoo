package io.quckoo.test

import akka.actor.{ActorRef, Props}
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}

/**
  * Created by domingueza on 12/07/2016.
  */
object ForwardActorSubscriber {

  def props(ref: ActorRef): Props = Props(classOf[ForwardActorSubscriber], ref)

}

class ForwardActorSubscriber(ref: ActorRef) extends ActorSubscriber {

  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override def receive: Receive = {
    case msg => ref forward msg
  }

}
