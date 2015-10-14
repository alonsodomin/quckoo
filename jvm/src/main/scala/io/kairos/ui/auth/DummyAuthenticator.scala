package io.kairos.ui.auth

import java.util.UUID

import scalaz.Scalaz._
import scalaz._

/**
 * Created by alonsodomin on 14/10/2015.
 */
class DummyAuthenticator extends Authenticator {

  override def authenticate(username: String, password: Array[Char]): Validation[AuthenticationError, String] =
    if (username == "admin" && password.mkString == "password")
      UUID.randomUUID().toString.success
    else AuthenticationError().failure

}
