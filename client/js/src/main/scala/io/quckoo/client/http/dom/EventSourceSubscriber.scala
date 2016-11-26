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

package io.quckoo.client.http.dom

import io.quckoo.client.http.HttpServerSentEvent
import io.quckoo.serialization.DataBuffer

import monix.execution.Cancelable
import monix.execution.cancelables.RefCountCancelable
import monix.reactive.observers.Subscriber

import org.scalajs.dom.raw.{Event, EventSource, MessageEvent}

import slogging.LazyLogging

/**
  * Created by alonsodomin on 02/04/2016.
  */
private[dom] class EventSourceSubscriber(url: String, eventType: String)
    extends (Subscriber.Sync[HttpServerSentEvent] => Cancelable) with LazyLogging {

  val source = new EventSource(url)

  override def apply(subscriber: Subscriber.Sync[HttpServerSentEvent]): Cancelable = {
    val cancelable = RefCountCancelable(source.close)

    source.onerror = (event: Event) => {
      logger.debug(s"Received 'error' event: $event")
      if (source.readyState == EventSource.CLOSED) {
        subscriber.onComplete()
      } else {
        subscriber.onError(new Exception(event.toString))
      }
      source.close()
    }

    source.addEventListener[MessageEvent](eventType, (message: MessageEvent) => {
      val data = DataBuffer.fromString(message.data.toString)
      subscriber.onNext(HttpServerSentEvent(data))
    })

    cancelable
  }

  def close(): Unit = source.close()

}
