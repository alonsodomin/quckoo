package io.quckoo.console.core

import io.quckoo.protocol.worker.WorkerEvent
import monifu.concurrent.Scheduler
import monifu.reactive.Ack.Continue
import monifu.reactive.{Ack, Observer, Subscriber}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 02/04/2016.
  */
class WorkerEventSubscriber extends Observer[WorkerEvent] {
  override def onError(ex: Throwable): Unit = ???

  override def onComplete(): Unit = ???

  override def onNext(elem: WorkerEvent): Future[Ack] = {
    ConsoleCircuit.dispatch(elem)
    Future.successful(Continue)
  }
}
