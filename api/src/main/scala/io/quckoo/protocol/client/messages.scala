package io.quckoo.protocol.client

import monocle.macros.Lenses

sealed trait ClientEvent
sealed trait ClientCommand

case object Connect extends ClientCommand
case object Connected extends ClientEvent

case object Disconnect extends ClientCommand
case object Disconnected extends ClientEvent
case object UnableToConnect extends ClientEvent

@Lenses
case class SignIn(username: String, password: String)
case object SignOut