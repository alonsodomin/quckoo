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

package io.quckoo.cluster.http

import upickle.default.{write, Writer => UWriter}

import akka.NotUsed
import akka.stream.scaladsl.Source

import de.heikoseeberger.akkasse.ServerSentEvent

import io.quckoo.api.EventDef
import io.quckoo.cluster.core.QuckooServer
import io.quckoo.serialization.json._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 29/09/2016.
  */
trait EventStream { this: QuckooServer =>

  def eventBus: Source[ServerSentEvent, _] = {
    def convertSSE[A: UWriter: EventDef](
        source: Source[A, NotUsed]): Source[ServerSentEvent, NotUsed] =
      source.map(evt => ServerSentEvent(write[A](evt), EventDef[A].typeName))

    val merged = convertSSE(masterEvents)
      .merge(convertSSE(workerEvents))
      .merge(convertSSE(registryEvents))
      .merge(convertSSE(schedulerEvents))
    merged.keepAlive(1 second, () => ServerSentEvent.heartbeat)
  }

}
