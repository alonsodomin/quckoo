package io.quckoo.client.http.dom

import io.quckoo.client.core.Channel
import io.quckoo.client.http.{HttpRequest, HttpResponse, dom, _}
import io.quckoo.serialization.DataBuffer
import monix.reactive.{Observable, OverflowStrategy}

import org.scalajs.dom.{XMLHttpRequest, Event => DOMEvent}
import org.scalajs.dom.ext.Ajax.InputData

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 09/09/2016.
  */
private[http] object HttpDOMBackend extends HttpBackend {

  final val ResponseType = "arraybuffer"

  override def open[Ch <: Channel[HttpProtocol]](channel: Ch) = Kleisli[Observable, Unit, HttpServerSentEvent] { _ =>
    val subscriber = new EventSourceSubscriber(EventsURI, channel.eventDef.typeName)
    Observable.create(OverflowStrategy.DropOld(20))(subscriber)
  }

  def send: Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    val timeout = {
      if (req.timeout.isFinite())
        req.timeout.toMillis.toInt
      else 0
    }

    val domReq = new XMLHttpRequest()
    val promise = Promise[HttpResponse]()

    domReq.onreadystatechange = { (e: DOMEvent) =>
      if (domReq.readyState == 4) {
        val entityData = DataBuffer(TypedArrayBuffer.wrap(domReq.response.asInstanceOf[ArrayBuffer]))
        val response = HttpResponse(domReq.status, domReq.statusText, entityData)
        promise.success(response)
      }
    }

    domReq.open(req.method.entryName, req.url)
    domReq.responseType = ResponseType
    domReq.timeout = timeout
    domReq.withCredentials = false
    req.headers.foreach(x => domReq.setRequestHeader(x._1, x._2))

    if (req.entity.isEmpty)
      domReq.send()
    else
      domReq.send(InputData.byteBuffer2ajax(req.entity.toByteBuffer))

    promise.future
  }

}
