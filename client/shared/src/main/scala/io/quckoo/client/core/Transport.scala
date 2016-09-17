package io.quckoo.client.core

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Transport[P <: Protocol] {
  val protocol: P

  def send(implicit ec: ExecutionContext): Kleisli[Future, protocol.Request, protocol.Response]
}
