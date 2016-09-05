package io.quckoo.client.internal.channel

import monix.reactive.Observable

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait ListenerChannel[Out] extends Channel[Out] {
  type M[X] = Observable[X]

  def bind(): Observable[Out]

}
