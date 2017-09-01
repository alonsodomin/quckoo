/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.client.core

import cats.data.Kleisli
import cats.instances.future._

import io.quckoo.api.TopicTag
import io.quckoo.serialization.Decoder
import io.quckoo.util._

import monix.reactive.Observable

import slogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 08/09/2016.
  */
final class Driver[P <: Protocol] private (
    private[client] val backend: DriverBackend[P],
    private[client] val specs: ProtocolSpecs[P]
) extends LazyLogging {

  private[client] def channelFor[E: TopicTag](implicit decoder: Decoder[String, E]) =
    specs.createChannel[E]

  def openChannel[E](ch: Channel.Aux[P, E]): Kleisli[Observable, Unit, ch.Event] = {
    logger.debug("Opening channel for topic: {}", ch.topicTag.name)
    def decodeEvent = ch.unmarshall.transform(attempt2Observable)
    backend.open(ch) andThen decodeEvent
  }

  def invoke[C <: CmdMarshalling[P]](implicit ec: ExecutionContext,
                                     cmd: C): Kleisli[Future, cmd.Cmd[cmd.In], cmd.Rslt] = {
    def encodeRequest  = cmd.marshall.transform(attempt2Future)
    def decodeResponse = cmd.unmarshall.transform(attempt2Future)

    encodeRequest andThen backend.send andThen decodeResponse
  }

}

object Driver {
  @inline implicit def apply[P <: Protocol](implicit backend: DriverBackend[P],
                                            commands: ProtocolSpecs[P]): Driver[P] =
    new Driver[P](backend, commands)
}
