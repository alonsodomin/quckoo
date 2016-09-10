package io.quckoo.client

import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.client.core.{Command, Driver, Protocol}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 10/09/2016.
  */
abstract class QuckooClientV2[P <: Protocol](driver: Driver[P]) {
  import driver.encodings._

  def authenticate(username: String, password: String)(implicit
    ec: ExecutionContext, timeout: Duration
  ): Future[Passport] = {
    val cmd = Command(Credentials(username, password), timeout, None)
    driver.invoke[Credentials, Passport].run(cmd)
  }

}
