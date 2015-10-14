package io.kairos.ui.auth

/**
 * Created by alonsodomin on 14/10/2015.
 */
protected[auth] object Auth {

  val XSRFTokenCookie = "XSRF_TOKEN"

  val XSRFTokenHeader = "X-" + XSRFTokenCookie

}
