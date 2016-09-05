package io.quckoo.client.internal.channel

import io.quckoo.client.internal.Request

import scala.concurrent.Future

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait SimpleChannel[In, Out] extends Channel[Out] {
  type M[X] = Future[X]

  def send(request: Request[In]): Future[Out]

}
