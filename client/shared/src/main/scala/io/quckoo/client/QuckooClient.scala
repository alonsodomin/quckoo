package io.quckoo.client

import io.quckoo.api.{Registry, Scheduler}
import io.quckoo.auth.User

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
trait QuckooClient extends Registry with Scheduler {

  def principal: User

  def close()(implicit ec: ExecutionContext): Future[Unit]

}
