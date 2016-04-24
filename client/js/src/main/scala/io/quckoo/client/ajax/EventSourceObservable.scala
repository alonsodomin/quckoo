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

package io.quckoo.client.ajax

import monix.execution.Cancelable
import monix.execution.cancelables.RefCountCancelable
import monix.reactive.observers.SyncSubscriber
import monix.reactive.{Observable, OverflowStrategy}
import org.scalajs.dom.raw.{Event, EventSource, MessageEvent}
import upickle.default._

/**
  * Created by alonsodomin on 02/04/2016.
  */
private[ajax] class EventSourceObservable[A: Reader] private (url: String, eventType: String)
    extends (SyncSubscriber[A] => Cancelable) {

  val source = new EventSource(url)

  override def apply(subscriber: SyncSubscriber[A]): Cancelable = {
    val cancelable = RefCountCancelable(source.close())

    source.onerror = (event: Event) => {
      if (source.readyState == EventSource.CLOSED) {
        subscriber.onComplete()
      } else {
        subscriber.onError(new Exception(event.toString))
      }
      source.close()
    }

    source.addEventListener[MessageEvent](eventType, (message: MessageEvent) => {
      val event = read[A](message.data.toString)
      subscriber.onNext(event)
      ()
    })

    cancelable
  }

  def close(): Unit = source.close()

}

private[ajax] object EventSourceObservable {

  def apply[A: Reader](url: String, eventType: String): Observable[A] =
    Observable.create[A](OverflowStrategy.DropOld(20))(new EventSourceObservable[A](url, eventType))

}
