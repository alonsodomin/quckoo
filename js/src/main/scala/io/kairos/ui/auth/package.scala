package io.kairos.ui

import scalaz.effect.IO

/**
 * Created by alonsodomin on 13/10/2015.
 */
package object auth {

  def isAuthenticated: IO[Boolean] = IO {
    Cookie.forName(Cookies.AuthTokenName).isDefined
  }

  def headers: Map[String, String] =
    Cookie.forName(Cookies.AuthTokenName).
      map(token => Map(Cookies.AuthTokenName -> token)).
      getOrElse(Map())

}
