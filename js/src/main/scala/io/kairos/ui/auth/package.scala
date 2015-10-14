package io.kairos.ui

import scalaz.effect.IO

/**
 * Created by alonsodomin on 13/10/2015.
 */
package object auth {

  def isAuthenticated: IO[Boolean] = IO {
    RootScope.cookie(Auth.XSRFTokenCookie).isDefined
  }

  def headers: Map[String, String] =
    RootScope.cookie(Auth.XSRFTokenCookie).
      map(token => Map(Auth.XSRFTokenHeader -> token)).
      getOrElse(Map())

}
