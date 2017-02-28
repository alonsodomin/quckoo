/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.client.http.dom

import io.quckoo.client.core.Channel
import io.quckoo.client.http.{HttpRequest, HttpResponse, _}
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

  override def open[Ch <: Channel[HttpProtocol]](channel: Ch) =
    Kleisli[Observable, Unit, HttpServerSentEvent] { _ =>
      val subscriber = new EventSourceSubscriber(EventsURI, channel.topicTag.name)
      Observable.create(OverflowStrategy.DropOld(20))(subscriber)
    }

  def send: Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    val timeout = {
      if (req.timeout.isFinite())
        req.timeout.toMillis.toInt
      else 0
    }

    val domReq  = new XMLHttpRequest()
    val promise = Promise[HttpResponse]()

    domReq.onreadystatechange = { (e: DOMEvent) =>
      if (domReq.readyState == 4) {
        val entityData =
          DataBuffer(TypedArrayBuffer.wrap(domReq.response.asInstanceOf[ArrayBuffer]))
        val response = HttpResponse(domReq.status, domReq.statusText, entityData)
        promise.success(response)
      }
    }

    domReq.open(req.method.entryName, req.url)
    domReq.responseType = ResponseType
    domReq.timeout = timeout.toDouble
    domReq.withCredentials = false
    req.headers.foreach(x => domReq.setRequestHeader(x._1, x._2))

    if (req.entity.isEmpty)
      domReq.send()
    else
      domReq.send(InputData.byteBuffer2ajax(req.entity.toByteBuffer))

    promise.future
  }

}
