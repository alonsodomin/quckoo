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

package io.quckoo.client.core

import io.quckoo.api.EventDef
import upickle.default.{Reader => UReader}

/**
  * Created by alonsodomin on 20/09/2016.
  */
trait ChannelMagnet[E] {
  implicit def eventDef: EventDef[E]
  implicit def decoder: UReader[E]

  def resolve[P <: Protocol](driver: Driver[P]): Channel.Aux[P, E] = driver.channelFor[E]
}

object ChannelMagnet {
  implicit def apply[E](implicit ev: EventDef[E], decoderEv: UReader[E]): ChannelMagnet[E] =
    new ChannelMagnet[E] {
      implicit val eventDef: EventDef[E] = ev
      implicit val decoder: UReader[E]   = decoderEv
    }
}
