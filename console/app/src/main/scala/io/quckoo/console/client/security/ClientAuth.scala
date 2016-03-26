package io.quckoo.console.client.security

import io.quckoo.auth._
import io.quckoo.console.client.core.DomScope
import io.quckoo.auth.http._
import japgolly.scalajs.react.CallbackTo

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ClientAuth {

  final def isAuthenticated: Boolean =
    DomScope.cookie(XSRFTokenCookie).isDefined

  final def authInfo: Option[AuthInfo] =
    DomScope.cookie(XSRFTokenCookie).map(AuthInfo(_))

  final def isAuthenticatedC: CallbackTo[Boolean] =
    CallbackTo { isAuthenticated }

}
