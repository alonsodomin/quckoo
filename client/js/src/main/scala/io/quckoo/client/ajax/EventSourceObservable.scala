package io.quckoo.client.ajax

import monifu.reactive.{Observable, Subscriber}
import org.scalajs.dom.raw.{Event, EventSource, MessageEvent}
import upickle.default._

/**
  * Created by alonsodomin on 02/04/2016.
  */
private[ajax] class EventSourceObservable[A: Reader] private (url: String, eventType: String)
    extends (Subscriber[A] => Unit) {

  val source = new EventSource(url)

  def apply(subscriber: Subscriber[A]): Unit = {
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
  }

  def close(): Unit = source.close()

}

private[ajax] object EventSourceObservable {

  def apply[A: Reader](url: String, eventType: String): Observable[A] =
    Observable.create(new EventSourceObservable[A](url, eventType))

}
