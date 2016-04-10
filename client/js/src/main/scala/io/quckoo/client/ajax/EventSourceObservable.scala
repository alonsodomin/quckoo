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
