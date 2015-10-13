package io.kairos.ui.auth

import scalaz.Validation

/**
 * Created by alonsodomin on 13/10/2015.
 */
trait Authenticator {

  def authenticate(username: String, password: Array[Char]): Validation[AuthenticationError, String]

}
