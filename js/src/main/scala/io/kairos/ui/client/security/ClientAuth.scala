package io.kairos.ui.client.security

import io.kairos.ui.auth.Auth
import io.kairos.ui.client.RootScope

import scalaz.effect.IO

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ClientAuth {

  def isAuthenticated: IO[Boolean] = IO {
    RootScope.cookie(Auth.XSRFTokenCookie).isDefined
  }

  def headers: Map[String, String] =
    RootScope.cookie(Auth.XSRFTokenCookie).
      map(token => Map(Auth.XSRFTokenHeader -> token)).
      getOrElse(Map())

}
