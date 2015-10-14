package io.kairos.ui

import java.util.UUID

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server._

/**
 * Created by alonsodomin on 14/10/2015.
 */
package object auth {
  import Directives.setCookie

  def addAuthCookie: Directive0 = {
    val token = UUID.randomUUID().toString
    setCookie(HttpCookie(Auth.XSRFTokenCookie, token, path = Some("/")))
  }

}
