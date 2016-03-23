package io.quckoo

/**
  * Created by alonsodomin on 21/03/2016.
  */
package object auth {

  val XSRFTokenCookie = "XSRF_TOKEN"
  val XSRFTokenHeader = "X-" + XSRFTokenCookie

  type UserId = String
  type Token = String

}
