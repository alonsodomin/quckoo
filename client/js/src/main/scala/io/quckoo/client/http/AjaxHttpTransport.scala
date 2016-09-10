package io.quckoo.client.http

import java.nio.ByteBuffer

import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.ext.Ajax.InputData

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.niocharset.StandardCharsets
import scalaz._

/**
  * Created by alonsodomin on 09/09/2016.
  */
object AjaxHttpTransport extends HttpTransport {

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
      data = null,
      timeout, req.headers,
      withCredentials = false,
      responseType = ""
    )

    ajaxCall.map { xhr =>
      if (xhr.status >= 400) {
        HttpError(xhr.status, xhr.statusText)
      } else {
        HttpSuccess(ByteBuffer.wrap(
          xhr.responseText.getBytes(StandardCharsets.UTF_8)
        ))
      }
    }
  }

}
