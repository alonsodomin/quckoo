package io.quckoo.auth

/**
  * Created by alonsodomin on 24/03/2016.
  */
package object http {

  final val XSRFTokenCookie = "XSRF_TOKEN"
  final val XSRFTokenHeader = "X-" + XSRFTokenCookie

  final val AuthScheme = "QuckooAuth"
  final val ApiTokenHeader = "X-QuckooToken"

}
