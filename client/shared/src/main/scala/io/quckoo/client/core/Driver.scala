package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.{ExecutionContext, Future}

import scalaz.Kleisli
import scalaz.std.scalaFuture._

/**
  * Created by alonsodomin on 08/09/2016.
  */
class Driver[P <: Protocol](private[client] val transport: Transport.For[P]) {
  import transport.protocol._

  private[client] val ops = transport.protocol.ops

  final def invoke[O <: Op](implicit ec: ExecutionContext, op: O): Kleisli[Future, op.Cmd[op.In], op.Rslt] = {
    def encodeRequest  = Kleisli(op.marshall).transform(lawfulTry2Future)
    def decodeResponse = Kleisli(op.unmarshall).transform(lawfulTry2Future)

    encodeRequest >=> transport.send >=> decodeResponse
  }

}
