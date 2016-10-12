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

package io.quckoo.cluster

import java.util.UUID

import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.{EventStreamElement, ServerSentEvent}
import upickle.default.{Writer => UWriter, write}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 24/03/2016.
  */
package object http {

  def asSSE[A: UWriter](
    source: Source[A, _], eventType: String,
    keepAlive: FiniteDuration = 1 second
  ): Source[EventStreamElement, _] = {
    source.map { event =>
      ServerSentEvent(write[A](event), eventType)
    } keepAlive(keepAlive, () => ServerSentEvent.Heartbeat)
  }

  def generateAuthToken = UUID.randomUUID().toString

}
