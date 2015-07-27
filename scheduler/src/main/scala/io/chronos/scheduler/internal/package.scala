package io.chronos.scheduler

import org.apache.ignite.lang.{IgniteAsyncSupport, IgniteFuture, IgniteInClosure}

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions
import scala.util.Try

/**
 * Created by aalonsodominguez on 27/07/15.
 */
package object internal {

  protected[internal] implicit def convertIgniteFuture[T](future: IgniteFuture[T])(implicit async: IgniteAsyncSupport): Future[T] = {
    val promise = Promise[T]()
    async.future[T]().listen(new IgniteInClosure[IgniteFuture[T]] {
      override def apply(e: IgniteFuture[T]): Unit =
        promise.complete(Try(e.get()))
    })
    promise.future
  }
  
}
