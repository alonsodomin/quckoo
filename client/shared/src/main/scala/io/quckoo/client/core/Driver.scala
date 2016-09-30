package io.quckoo.client.core

import upickle.default.{Reader => UReader}

import io.quckoo.api.EventDef
import io.quckoo.util._

import monix.reactive.Observable
import monix.scalaz._

import slogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

import scalaz.Kleisli
import scalaz.std.scalaFuture._

/**
  * Created by alonsodomin on 08/09/2016.
  */
final class Driver[P <: Protocol] private (
    private[client] val backend: DriverBackend[P],
    private[client] val specs: ProtocolSpecs[P]
  ) extends LazyLogging {

  private[client] def channelFor[E : EventDef : UReader] = specs.createChannel[E]

  def openChannel[E](ch: Channel.Aux[P, E]): Kleisli[Observable, Unit, ch.Event] = {
    logger.debug(s"Opening channel for event ${ch.eventDef.typeName}")
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
  @inline implicit def apply[P <: Protocol](implicit backend: DriverBackend[P], commands: ProtocolSpecs[P]): Driver[P] =
    new Driver[P](backend, commands)
}
