package io.quckoo.client.core

import upickle.default.{Reader => UReader}

import io.quckoo.util._

import monix.reactive.Observable
import monix.scalaz._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli
import scalaz.std.scalaFuture._

/**
  * Created by alonsodomin on 08/09/2016.
  */
final class Driver[P <: Protocol] private (
    private[client] val backend: DriverBackend[P],
    private[client] val specs: ProtocolSpecs[P]
  ) {

  private[client] def channelFor[E : EventDef : UReader] = specs.createChannel[E]

  def subscribeOn[E](ch: Channel.Aux[P, E]): Kleisli[Observable, Unit, ch.Event] = {
    def decodeEvent = ch.unmarshall.transform(lawfulTry2Observable)
    backend.open(ch) >=> decodeEvent
  }

  def invoke[C <: CmdMarshalling[P]](implicit ec: ExecutionContext, cmd: C): Kleisli[Future, cmd.Cmd[cmd.In], cmd.Rslt] = {
    def encodeRequest  = cmd.marshall.transform(lawfulTry2Future)
    def decodeResponse = cmd.unmarshall.transform(lawfulTry2Future)

    encodeRequest >=> backend.send >=> decodeResponse
  }

}

object Driver {
  @inline def apply[P <: Protocol](implicit transport: DriverBackend[P], commands: ProtocolSpecs[P]): Driver[P] =
    new Driver[P](transport, commands)
}
