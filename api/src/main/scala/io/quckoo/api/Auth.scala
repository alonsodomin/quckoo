package io.quckoo.api

import io.quckoo.auth.AuthInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait Auth {

  def authenticate(username: String, password: Array[Char])(implicit ec: ExecutionContext): Future[Option[AuthInfo]]

}
