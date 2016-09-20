package io.quckoo.client.core

import monix.reactive.Observable

import scala.concurrent.Future
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait DriverBackend[P <: Protocol] {
  def subscribe[Ch <: Channel[P]]: Kleisli[Observable, String, Ch#Event] = ???
  def send: Kleisli[Future, P#Request, P#Response]
}
