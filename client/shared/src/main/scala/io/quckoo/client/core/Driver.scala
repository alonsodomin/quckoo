package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 08/09/2016.
  */
abstract class Driver[P <: Protocol](private[client] val transport: Transport[P]) {
  import transport.protocol._

  val ops = transport.protocol.ops

  final def invoke[O <: Op](implicit ec: ExecutionContext, op: O): Kleisli[Future, op.Cmd[op.In], op.Rslt] = {
    def encodeRequest  = Kleisli(op.marshall).transform(either2Future)
    def decodeResponse = Kleisli(op.unmarshall).transform(either2Future)

    val execute = encodeRequest >=> transport.send >=> decodeResponse
    execute.mapT(_.recover(op.recover))
  }

}
