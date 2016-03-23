package io.quckoo.console.client.security

import io.quckoo.auth._
import io.quckoo.console.client.core.RootScope
import japgolly.scalajs.react.CallbackTo

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ClientAuth {

  final def isAuthenticated: Boolean =
    RootScope.cookie(XSRFTokenCookie).isDefined

  final def authInfo: Option[AuthInfo] =
    RootScope.cookie(XSRFTokenCookie).map(AuthInfo(_))

  final def isAuthenticatedC: CallbackTo[Boolean] =
    CallbackTo { isAuthenticated }

  final def authHeaders: Map[String, String] =
    RootScope.cookie(XSRFTokenCookie).
      map(token => Map(XSRFTokenHeader -> token)).
      getOrElse(Map())

}
