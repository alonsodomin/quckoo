package io.kairos.protocol

trait ClientEvent

case object Connect
case object Connected extends ClientEvent

case object Disconnect
case object Disconnected extends ClientEvent

case object UnableToConnect extends ClientEvent