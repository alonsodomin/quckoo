package io.quckoo.cluster

import java.util.UUID

import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent
import upickle.default._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 24/03/2016.
  */
package object http {

  def asSSE[A: Writer](source: Source[A, _], eventType: String,
                       keepAlive: FiniteDuration = 1 second): Source[ServerSentEvent, _] = {
    source.map { event =>
      ServerSentEvent(write[A](event), eventType)
    } keepAlive(keepAlive, () => ServerSentEvent.heartbeat)
  }

  def generateAuthToken = UUID.randomUUID().toString

}
