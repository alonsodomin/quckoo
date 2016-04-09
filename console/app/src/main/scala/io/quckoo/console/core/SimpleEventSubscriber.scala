package io.quckoo.console.core

import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.Observer
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
