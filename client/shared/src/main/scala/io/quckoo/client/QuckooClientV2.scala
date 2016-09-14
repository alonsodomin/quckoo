package io.quckoo.client

import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.client.core.{AnonCmd, AuthCmd, Driver, Protocol}
import io.quckoo.net.QuckooState

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 10/09/2016.
  */
abstract class QuckooClientV2[P <: Protocol](driver: Driver[P]) {
  import driver.ops._

  def authenticate(username: String, password: String)(implicit
    ec: ExecutionContext, timeout: Duration
  ): Future[Passport] = {
    val cmd = AnonCmd(Credentials(username, password), timeout)
    driver.invoke[AnonCmd, Credentials, Passport].run(cmd)
  }

  def clusterState(implicit ec: ExecutionContext, timeout: Duration, passport: Passport): Future[QuckooState] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[AuthCmd, Unit, QuckooState].run(cmd)
  }

}
