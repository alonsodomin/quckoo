package io.chronos.protocol

import akka.actor.ActorRef

/**
 * Created by aalonsodominguez on 13/07/15.
 */
object ListenerProtocol {

  case class Subscribe(subscriber: ActorRef)
  case object SubscribeAck

  case class Unsubscribe(subscriber: ActorRef)
  case object UnsubscribeAck

}
