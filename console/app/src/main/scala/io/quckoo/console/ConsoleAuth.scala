package io.quckoo.console

import io.quckoo.auth.AuthInfo
import io.quckoo.protocol.cluster.ClusterInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
trait ConsoleAuth {

  def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Option[AuthInfo]]

  def logout()(implicit ec: ExecutionContext, auth: AuthInfo): Future[Unit]

  def clusterDetails(implicit ec: ExecutionContext, auth: AuthInfo): Future[ClusterInfo]

}
