package io.quckoo.client.ajax

import org.reactivestreams.{Publisher, Subscriber}
import org.scalajs.dom._
import org.scalajs.dom.raw.{EventSource, Event, MessageEvent}
import upickle.default._

/**
  * Created by alonsodomin on 02/04/2016.
  */
private[ajax] class EventSourcePublisher[A: Reader](url: String, eventType: String) extends (Subscriber[A] => Unit) {

  val source = new EventSource(url)

  def apply(subscriber: Subscriber[A]): Unit = {
    source.onerror = (event: Event) => {
      if (source.readyState == EventSource.CLOSED) {
        subscriber.onComplete()
      } else {
        subscriber.onError(new Exception(event.toString))
      }
    }

    source.addEventListener[MessageEvent](eventType, (message: MessageEvent) => {
      val sse = read[ServerSentEvent](message.data.toString)
      val event = read[A](sse.data)
      subscriber.onNext(event)
      ()
    })
  }

}
