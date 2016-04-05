package io.quckoo.console.core

import monifu.reactive.Ack.Continue
import monifu.reactive.{Ack, Observer}
import org.scalajs.dom.console

import scala.concurrent.Future

/**
  * Created by alonsodomin on 02/04/2016.
  */
class SimpleEventSubscriber[A <: AnyRef] extends Observer[A] {

  override def onError(ex: Throwable): Unit = {
    console.log(s"${ex.getClass.getName}: ${ex.getMessage}")
  }

  override def onComplete(): Unit = ()

  override def onNext(elem: A): Future[Ack] = {
    ConsoleCircuit.dispatch(elem)
    Future.successful(Continue)
  }

}
