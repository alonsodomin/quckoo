package io.quckoo.client.ajax

import io.quckoo.auth.http._
import io.quckoo.client.{QuckooClient, QuckooClientFactory}
import io.quckoo.protocol.client.SignIn
import io.quckoo.serialization.Base64._

import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
object AjaxQuckooClientFactory extends QuckooClientFactory {

  def autoConnect(): Option[QuckooClient] = Cookie(AuthCookie).map(token => new AjaxQuckooClient(token))

  def connect(username: String, password: String)(implicit ec: ExecutionContext): Future[QuckooClient] = {
    val authentication = s"$username:$password".getBytes("UTF-8").toBase64
    val hdrs = Map(AuthorizationHeader -> s"Basic $authentication")

    Ajax.post(LoginURI, write(SignIn(username, password.toCharArray)), headers = hdrs, withCredentials = true).
      map { xhr => new AjaxQuckooClient(xhr.responseText) }
  }

}
