package io.quckoo.client.core

import io.quckoo.api.EventDef
import upickle.default.{Reader => UReader}

/**
  * Created by alonsodomin on 20/09/2016.
  */
trait ChannelMagnet[E] {
  implicit def eventDef: EventDef[E]
  implicit def decoder: UReader[E]

  def resolve[P <: Protocol](driver: Driver[P]): Channel.Aux[P, E] = driver.channelFor[E]
}

object ChannelMagnet {
  implicit def apply[E](implicit ev: EventDef[E], decoderEv: UReader[E]): ChannelMagnet[E] = new ChannelMagnet[E] {
    implicit val eventDef: EventDef[E] = ev
    implicit val decoder: UReader[E] = decoderEv
  }
}
