package io.kairos.ui

import io.kairos.ui.protocol.{LoginRequest, LoginResponse}
import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
object ClientApi extends Api {

  private[this] val BaseURI = "/api"
  private[this] val LoginURI = BaseURI + "/login"

  private[this] val JsonRequestHeaders = Map("Content-Type" -> "application/json")

  override def login(username: String, password: String)(implicit ec: ExecutionContext): Future[String] = {
    import upickle.default._

    val request = LoginRequest(username, password)
    Ajax.post(LoginURI, write(request), headers = JsonRequestHeaders ++ auth.headers).map { xhr =>
      read[LoginResponse](xhr.responseText)
    } map { res => res.token }
  }

}
