package io.chronos.protocol

/**
 * Created by aalonsodominguez on 13/07/15.
 */
object ListenerProtocol {

  case object Subscribe
  case object SubscribeAck

  case object Unsubscribe
  case object UnsubscribeAck

}
