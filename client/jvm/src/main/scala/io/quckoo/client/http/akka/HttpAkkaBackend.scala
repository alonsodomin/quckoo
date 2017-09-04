/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.client.http.akka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethods}
import akka.http.scaladsl.model.{
  HttpMethod => AkkaHttpMethod,
  HttpRequest => AkkaHttpRequest,
  HttpResponse => AkkaHttpResponse
}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http => AkkaHttp}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

import cats.data.Kleisli

import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent
import de.heikoseeberger.akkasse.scaladsl.unmarshalling.EventStreamUnmarshalling

import io.quckoo.client.core.Channel
import io.quckoo.client.http.{HttpMethod1, HttpRequest1, HttpResponse1, _}
import io.quckoo.serialization.DataBuffer

import monix.reactive.Observable

import scala.collection.immutable
import scala.concurrent.Future

/**
  * Created by alonsodomin on 11/09/2016.
  */
private[http] final class HttpAkkaBackend(host: String,
                                          port: Int = 80)(implicit val actorSystem: ActorSystem)
    extends HttpBackend {

  implicit val materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem), "quckoo-http")

  val connection = AkkaHttp().outgoingConnection(host, port)

  override def open[Ch <: Channel[HttpProtocol]](channel: Ch) =
    Kleisli[Observable, Unit, HttpServerSentEvent] { _ =>
      import EventStreamUnmarshalling._
      import actorSystem.dispatcher

      val publisherSink = Sink.asPublisher[ServerSentEvent](fanout = true)

      val source = for {
        response <- AkkaHttp().singleRequest(
          Get(s"http://$host:$port" + topicURI(channel.topicTag.name))
        )
        events <- Unmarshal(response).to[Source[ServerSentEvent, NotUsed]]
      } yield events.runWith(publisherSink)

      for {
        publisher <- Observable.fromFuture(source)
        event     <- Observable.fromReactivePublisher(publisher)
      } yield {
        HttpServerSentEvent(DataBuffer.fromString(event.toString))
      }
    }

  override def send: Kleisli[Future, HttpRequest1, HttpResponse1] = Kleisli {
    req =>
      def method: AkkaHttpMethod = req.method match {
        case HttpMethod1.Get    => HttpMethods.GET
        case HttpMethod1.Put    => HttpMethods.PUT
        case HttpMethod1.Post   => HttpMethods.POST
        case HttpMethod1.Delete => HttpMethods.DELETE
      }

    val headers = {
      req.headers
        .filterKeys(_ != "Content-Type")
        .map({
          case (name, value) => HttpHeader.parse(name, value)
        })
        .flatMap {
          case ParsingResult.Ok(header, _) => Seq(header)
          case _                           => Seq()
        }
        .to[immutable.Seq]
    }

      def parseRawResponse(
          response: AkkaHttpResponse): Future[HttpResponse1] = {
        val entityData = response.entity.dataBytes.runFold(ByteString())(_ ++ _)

        import actorSystem.dispatcher
        entityData.map(
          buff =>
            HttpResponse1(response.status.intValue(),
                          response.status.value,
                          DataBuffer.fromString(buff.utf8String)))
      }

      val entity =
        HttpEntity(ContentTypes.`application/json`, req.entity.asString())
      Source
        .single(
          AkkaHttpRequest(method,
                          uri = req.url,
                          entity = entity,
                          headers = headers))
        .via(connection)
        .mapAsync(1)(parseRawResponse)
        .runWith(Sink.head[HttpResponse1])
  }
}
