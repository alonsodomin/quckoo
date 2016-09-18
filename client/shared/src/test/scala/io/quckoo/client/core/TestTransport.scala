package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.Future

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 17/09/2016.
  */
private[core] final class TestTransport[P0 <: Protocol](private[client] val protocol: P0) extends Transport {
  type P = P0

  private[this] var logic: protocol.Request => LawfulTry[protocol.Response] = { _ =>
    new IllegalStateException("TestTransport needs to be lifted before being used!").left[protocol.Response]
  }

  def lift(f: protocol.Request => LawfulTry[protocol.Response]): TestTransport[P] = {
    logic = f
    this
  }

  override def send: Kleisli[Future, protocol.Request, protocol.Response] =
    Kleisli[LawfulTry, protocol.Request, protocol.Response](logic).transform(lawfulTry2Future)

}
