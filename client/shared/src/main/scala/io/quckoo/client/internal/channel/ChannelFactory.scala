package io.quckoo.client.internal.channel

import io.quckoo.client.internal.{Protocol, Request, Transport}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait ChannelFactory[P <: Protocol, R] {
  type Ch[_] <: Channel[_]
  type Out

  def channel[T <: Transport[P]](transport: T): Ch[Out]

}

object ChannelFactory {

  def simple[P <: Protocol, In, Out0]
      (f: Request[In] => Future[Out0]) = new ChannelFactory[P, In] {
    type Ch[X] = SimpleChannel[In, X]
    type Out = Out0

    override def channel[T <: Transport[P]](transport: T) = new SimpleChannel[In, Out0] {

      override def send(reqCtx: Request[In]) = f(reqCtx)
    }
  }

}