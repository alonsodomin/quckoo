package io.quckoo.cluster.http

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import io.quckoo.auth.XSRFToken
import io.quckoo.auth.http

import scala.util.Try

/**
  * Created by alonsodomin on 29/03/2016.
  */
object ApiTokenHeader extends ModeledCustomHeaderCompanion[ApiTokenHeader] {

  override val name: String = http.ApiTokenHeader

  override def parse(value: String): Try[ApiTokenHeader] = ???

}

class ApiTokenHeader(token: XSRFToken) extends ModeledCustomHeader[ApiTokenHeader] {

  override val companion: ModeledCustomHeaderCompanion[ApiTokenHeader] = ApiTokenHeader

  override def value(): String = token.toString

  override def renderInResponses(): Boolean = ???

  override def renderInRequests(): Boolean = ???
}
