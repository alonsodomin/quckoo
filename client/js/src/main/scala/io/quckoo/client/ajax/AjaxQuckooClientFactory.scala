package io.quckoo.client.ajax

import io.quckoo.client.{QuckooClient, QuckooClientFactory}
import io.quckoo.protocol.client.SignIn

import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
object AjaxQuckooClientFactory extends QuckooClientFactory {

  def autoConnect(): Option[QuckooClient] = authInfo.map(_ => AjaxQuckooClient)

  def connect(username: String, password: String)(implicit ec: ExecutionContext): Future[QuckooClient] = {
    Ajax.post(LoginURI, write(SignIn(username, password.toCharArray)), headers = JsonRequestHeaders).
      map { _ => AjaxQuckooClient }
  }

}
