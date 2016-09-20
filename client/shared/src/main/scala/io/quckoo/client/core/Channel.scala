package io.quckoo.client.core

/**
  * Created by domingueza on 20/09/2016.
  */
trait Channel[P <: Protocol] {
  type Event

  val uri: String
  val unmarshall: Unmarshall[P#EventType, Event]
}

object Channel {
  trait Aux[P <: Protocol, E] extends Channel[P] { type Event = E }
}