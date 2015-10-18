package io.kairos.console.client.security

import io.kairos.console.auth.Auth
import io.kairos.console.client.core.RootScope

import scalaz.effect.IO

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ClientAuth {

  def isAuthenticated: Boolean =
    RootScope.cookie(Auth.XSRFTokenCookie).isDefined

  def isAuthenticatedIO: IO[Boolean] = IO {
    RootScope.cookie(Auth.XSRFTokenCookie).isDefined
  }

  def authHeaders: Map[String, String] =
    RootScope.cookie(Auth.XSRFTokenCookie).
      map(token => Map(Auth.XSRFTokenHeader -> token)).
      getOrElse(Map())

}
