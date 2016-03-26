package io.quckoo.client

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
trait QuckooClientFactory {

  def connect(username: String, password: String)(implicit ec: ExecutionContext): Future[QuckooClient]

}
