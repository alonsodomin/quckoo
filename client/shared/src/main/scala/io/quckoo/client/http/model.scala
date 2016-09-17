package io.quckoo.client.http

import enumeratum.EnumEntry._
import enumeratum._
import io.quckoo.serialization.DataBuffer

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
  entity: DataBuffer = DataBuffer.Empty
)

final case class HttpResponse(statusCode: Int, statusLine: String, entity: DataBuffer = DataBuffer.Empty) {
  def isFailure: Boolean = statusCode >= 400
  def isSuccess: Boolean = !isFailure
}
final case class HttpErrorException(statusLine: String) extends Exception(statusLine)