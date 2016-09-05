package io.quckoo.client

import io.quckoo.auth.Passport

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait QuckooClientV2 {

  def authenticate(username: String, password: String)(implicit
    ec: ExecutionContext, timeout: Duration
  ): Future[Passport]

}
