package io.quckoo.client.core

import scala.concurrent.Future
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Transport[P <: Protocol] {
  def send: Kleisli[Future, P#Request, P#Response]
}
