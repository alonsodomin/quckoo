package io.quckoo.client.core

import scala.concurrent.Future
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Transport {
  type P <: Protocol

  private[client] val protocol: P

  def send: Kleisli[Future, protocol.Request, protocol.Response]
}

object Transport {
  type For[P0 <: Protocol] = Transport { type P = P0 }
}