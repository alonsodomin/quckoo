package io.quckoo.cluster.http

import upickle.default.{write, Writer => UWriter}

import akka.NotUsed
import akka.stream.scaladsl.Source

import de.heikoseeberger.akkasse.{EventStreamElement, ServerSentEvent}

import io.quckoo.api.EventDef
import io.quckoo.cluster.core.QuckooServer

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 29/09/2016.
  */
trait EventStream { this: QuckooServer =>

  def eventBus: Source[EventStreamElement, _] = {
    def convertSSE[A: UWriter: EventDef](source: Source[A, NotUsed]): Source[ServerSentEvent, NotUsed] =
      source.map(evt => ServerSentEvent(write[A](evt), EventDef[A].typeName))

    val merged = convertSSE(masterEvents).
      merge(convertSSE(workerEvents)).
      merge(convertSSE(registryEvents)).
      merge(convertSSE(schedulerEvents))
    merged.keepAlive(1 second, () => ServerSentEvent.Heartbeat)
  }

}
