package io.quckoo.client.core

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Transport[P <: Protocol] {
  type Request
  type Response

  def send(implicit ec: ExecutionContext): Kleisli[Future, Request, Response]

}
