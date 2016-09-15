package io.quckoo.client.http

import enumeratum.EnumEntry._
import enumeratum._

import scala.concurrent.duration.Duration

/**
  * Created by alonsodomin on 10/09/2016.
  */

sealed trait HttpMethod extends EnumEntry with Uppercase
object HttpMethod extends Enum[HttpMethod] {
  val values = findValues

  case object Get extends HttpMethod
  case object Put extends HttpMethod
  case object Post extends HttpMethod
  case object Delete extends HttpMethod
}

final case class HttpRequest(
  method: HttpMethod,
  url: String,
  timeout: Duration,
  headers: Map[String, String],
  entity: Option[HttpEntity]
)

sealed trait HttpResponse
final case class HttpError(statusCode: Int, statusLine: String) extends HttpResponse
final case class HttpSuccess(entity: HttpEntity) extends HttpResponse

final case class HttpErrorException(error: HttpError) extends Exception(error.statusLine)