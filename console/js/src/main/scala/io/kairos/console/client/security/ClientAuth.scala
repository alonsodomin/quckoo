package io.kairos.console.client.security

import diode.data.Empty
import io.kairos.console.auth.{AuthInfo, Auth}
import io.kairos.console.client.core.{KairosCircuit, RootScope}
import japgolly.scalajs.react.CallbackTo

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ClientAuth {

  final def isAuthenticated: Boolean =
    RootScope.cookie(Auth.XSRFTokenCookie).isDefined

  final def authInfo: Option[AuthInfo] =
    RootScope.cookie(Auth.XSRFTokenCookie).map(AuthInfo(_))

  final def isAuthenticatedC: CallbackTo[Boolean] =
    CallbackTo { isAuthenticated }

  final def authHeaders: Map[String, String] =
    RootScope.cookie(Auth.XSRFTokenCookie).
      map(token => Map(Auth.XSRFTokenHeader -> token)).
      getOrElse(Map())

}
