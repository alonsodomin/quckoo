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

package io.quckoo.client.http.dom

import io.quckoo.client.core.ChannelException
import io.quckoo.client.http.{HttpServerSentEvent, topicURI}
import io.quckoo.serialization.DataBuffer

import monix.execution.Cancelable
import monix.execution.cancelables.RefCountCancelable
import monix.reactive.observers.Subscriber

import org.scalajs.dom.raw.{Event, EventSource, MessageEvent}

import slogging.LazyLogging

/**
  * Created by alonsodomin on 02/04/2016.
  */
private[dom] class EventSourceSubscriber(topicName: String)
    extends (Subscriber.Sync[HttpServerSentEvent] => Cancelable) with LazyLogging {

  val topicURL: String = topicURI(topicName)

  logger.debug("Subscribing to topic '{}' using URL: {}", topicName, topicURL)

  val source = new EventSource(topicURL)

  override def apply(subscriber: Subscriber.Sync[HttpServerSentEvent]): Cancelable = {
    val cancelable = RefCountCancelable { () =>
      subscriber.onComplete()
      source.close
    }

    source.onerror = (event: Event) => {
      if (source.readyState == EventSource.CLOSED) {
        subscriber.onComplete()
      } else {
        subscriber.onError(new ChannelException(topicName))
      }
      source.close()
    }

    source
      .addEventListener[MessageEvent](topicName, (message: MessageEvent) => {
        val data = DataBuffer.fromString(message.data.toString)
        subscriber.onNext(HttpServerSentEvent(data))
      })

    cancelable
  }

}
