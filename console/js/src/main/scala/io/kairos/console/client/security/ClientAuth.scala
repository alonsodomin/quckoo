package io.kairos.console.client.security

import io.kairos.console.auth.Auth
import io.kairos.console.client.core.RootScope
import japgolly.scalajs.react.CallbackTo

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ClientAuth {

  def isAuthenticated: Boolean =
    RootScope.cookie(Auth.XSRFTokenCookie).isDefined

  def isAuthenticatedC: CallbackTo[Boolean] =
    CallbackTo { isAuthenticated }

  def authHeaders: Map[String, String] =
    RootScope.cookie(Auth.XSRFTokenCookie).
      map(token => Map(Auth.XSRFTokenHeader -> token)).
      getOrElse(Map())

}
