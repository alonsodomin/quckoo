/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.client.http

import io.quckoo.api.EventDef
import upickle.default.{Reader => UReader}
import io.quckoo.client.core.{Channel, Channels, Unmarshall}

/**
  * Created by domingueza on 20/09/2016.
  */
trait SSEChannels extends Channels[HttpProtocol] {

  override def createChannel[E: EventDef: UReader] = new Channel.Aux[HttpProtocol, E] {
    override val eventDef   = implicitly[EventDef[E]]
    override val unmarshall = Unmarshall[HttpServerSentEvent, E](_.data.as[E])
  }

}
