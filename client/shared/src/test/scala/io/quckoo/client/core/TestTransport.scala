package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.Future

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 17/09/2016.
  */
final class TestTransport[P0 <: Protocol](val protocol: P0) extends Transport {
  type P = P0

  private[this] var logic: protocol.Request => LawfulTry[protocol.Response] = { _ =>
    new RuntimeException("Not implemented").left[protocol.Response]
  }

  def lift(f: protocol.Request => LawfulTry[protocol.Response]): TestTransport[P] = {
    logic = f
    this
  }

  override def send: Kleisli[Future, protocol.Request, protocol.Response] =
    Kleisli[LawfulTry, protocol.Request, protocol.Response](logic).transform(lawfulTry2Future)

}
