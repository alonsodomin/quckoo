package io.kairos.protocol

object ClientProtocol {

  sealed trait ClientEvent
  sealed trait ClientCommand

  case object Connect extends ClientCommand

  case object Connected extends ClientEvent

  case object Disconnect extends ClientCommand

  case object Disconnected extends ClientEvent

  case object UnableToConnect extends ClientEvent

}