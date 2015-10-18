package io.kairos.console

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
trait Api {

  def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Unit]

  def logout()(implicit ec: ExecutionContext): Future[Unit]

}
