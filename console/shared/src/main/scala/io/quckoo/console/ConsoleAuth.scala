package io.quckoo.console

import io.quckoo.console.info.ClusterInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
trait ConsoleAuth {

  def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Unit]

  def logout()(implicit ec: ExecutionContext): Future[Unit]

  def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo]

}
