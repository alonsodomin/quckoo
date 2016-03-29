package io.quckoo.cluster.core

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}

import scala.annotation.tailrec

/**
  * Created by alonsodomin on 28/03/2016.
  */
object EventPublisher {

  final val DefaultBufferSize = 100

}
abstract class EventPublisher[A](bufferSize: Int = EventPublisher.DefaultBufferSize)
    extends ActorPublisher[A] {

  var eventBuffer = Vector.empty[A]

  override def unhandled(message: Any): Unit = {
    message match {
      case Request(_) =>
        deliverEvents()

      case Cancel =>
        context.stop(self)

      case _ =>
        super.unhandled(message)
    }
  }

  protected final def emitEvent(event: A): Unit = {
    if (eventBuffer.isEmpty && totalDemand > 0) {
      onNext(event)
    } else {
      if (eventBuffer.size == bufferSize) {
        eventBuffer = eventBuffer.drop(1)
      }
      eventBuffer :+= event
      deliverEvents()
    }
  }

  @tailrec
  private[this] def deliverEvents(): Unit = if (totalDemand > 0) {
    def dispatchBufferItems(itemCount: Int): Unit = {
      val (head, tail) = eventBuffer.splitAt(itemCount)
      eventBuffer = tail
      head foreach onNext
    }

    val dispatchCount = if (totalDemand <= Int.MaxValue)
      totalDemand.toInt
    else
      Int.MaxValue

    dispatchBufferItems(dispatchCount)
    if (dispatchCount == Int.MaxValue) deliverEvents()
  }

}
