package io.quckoo.client.http

import java.nio.ByteBuffer

import io.quckoo.serialization.DataBuffer
import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}
import scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scalaz._

/**
  * Created by alonsodomin on 09/09/2016.
  */
private[http] object AjaxTransport extends HttpTransport {

  final val ResponseType = "arraybuffer"

  def send(implicit ec: ExecutionContext): Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    val timeout = {
      if (req.timeout.isFinite())
        req.timeout.toMillis.toInt
      else 0
    }

    val ajaxCall = Ajax(
      method = req.method.entryName,
      url = req.url,
      data = req.entity.map(DataBuffer.toByteBuffer).orNull[ByteBuffer],
      timeout, req.headers,
      withCredentials = false,
      responseType = ResponseType
    )

    ajaxCall.map { xhr =>
      if (xhr.status >= 400) {
        HttpError(xhr.status, xhr.statusText)
      } else {
        val bytes = TypedArrayBuffer.wrap(xhr.response.asInstanceOf[ArrayBuffer])
        HttpSuccess(DataBuffer(bytes))
      }
    }
  }

}
