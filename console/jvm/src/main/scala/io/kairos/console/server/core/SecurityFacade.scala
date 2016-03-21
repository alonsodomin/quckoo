package io.kairos.console.server.core

import io.kairos.console.auth.AuthInfo

import scala.concurrent.Future

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait SecurityFacade {

  def authenticate(username: String, password: Array[Char]): Future[Option[AuthInfo]]

}
