package io.quckoo.client.http

import upickle.default.{Reader => UReader}

import io.quckoo.client.core.{Channel, Channels, EventDef, Unmarshall}

/**
  * Created by domingueza on 20/09/2016.
  */
trait SSEChannels extends Channels[HttpProtocol] {

  override def createChannel[E: EventDef : UReader] = new Channel.Aux[HttpProtocol, E] {
    override val eventDef = implicitly[EventDef[E]]
    override val unmarshall = Unmarshall[HttpServerSentEvent, E](_.data.as[E])
  }

}
