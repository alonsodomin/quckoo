package io.kairos.ui.auth

/**
 * Created by alonsodomin on 14/10/2015.
 */
object Auth {

  val UsernameCookie = "Principal"

  val XSRFTokenCookie = "XSRF_TOKEN"

  val XSRFTokenHeader = "X-" + XSRFTokenCookie

}
