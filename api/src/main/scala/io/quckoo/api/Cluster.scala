package io.quckoo.api

import io.quckoo.net.QuckooState

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 04/04/2016.
  */
trait Cluster {

  def clusterState(implicit ec: ExecutionContext): Future[QuckooState]

}
