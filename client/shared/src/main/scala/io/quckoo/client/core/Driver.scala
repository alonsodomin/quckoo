package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.{ExecutionContext, Future}

import scalaz.Kleisli
import scalaz.std.scalaFuture._

/**
  * Created by alonsodomin on 08/09/2016.
  */
final class Driver[P <: Protocol] private (
    private[client] val transport: Transport[P],
    private[client] val commands: AllProtocolCmds[P]
  ) {

  def invoke[O <: CmdMarshalling[P]](implicit ec: ExecutionContext, op: O): Kleisli[Future, op.Cmd[op.In], op.Rslt] = {
    def encodeRequest  = op.marshall.transform(lawfulTry2Future)
    def decodeResponse = op.unmarshall.transform(lawfulTry2Future)

    encodeRequest >=> transport.send >=> decodeResponse
  }

}

object Driver {
  @inline def apply[P <: Protocol](implicit transport: Transport[P], commands: AllProtocolCmds[P]): Driver[P] =
    new Driver[P](transport, commands)
}
