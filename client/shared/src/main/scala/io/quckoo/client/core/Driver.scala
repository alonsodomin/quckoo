package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.{ExecutionContext, Future}

import scalaz.Kleisli
import scalaz.std.scalaFuture._

/**
  * Created by alonsodomin on 08/09/2016.
  */
final class Driver[P <: Protocol] private (
                                            private[client] val transport: DriverBackend[P],
                                            private[client] val commands: AllProtocolCmds[P]
  ) {

  def invoke[C <: CmdMarshalling[P]](implicit ec: ExecutionContext, cmd: C): Kleisli[Future, cmd.Cmd[cmd.In], cmd.Rslt] = {
    def encodeRequest  = cmd.marshall.transform(lawfulTry2Future)
    def decodeResponse = cmd.unmarshall.transform(lawfulTry2Future)

    encodeRequest >=> transport.send >=> decodeResponse
  }

}

object Driver {
  @inline def apply[P <: Protocol](implicit transport: DriverBackend[P], commands: AllProtocolCmds[P]): Driver[P] =
    new Driver[P](transport, commands)
}
