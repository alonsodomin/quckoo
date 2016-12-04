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

    val dispatchCount =
      if (totalDemand <= Int.MaxValue) {
        totalDemand.toInt
      } else {
        Int.MaxValue
      }

    dispatchBufferItems(dispatchCount)
    if (dispatchCount == Int.MaxValue) deliverEvents()
  }

}
