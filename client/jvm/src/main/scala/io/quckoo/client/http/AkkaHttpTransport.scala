package io.quckoo.client.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpMethod => AkkaHttpMethod, HttpRequest => AkkaHttpRequest, HttpResponse => AkkaHttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import io.quckoo.serialization.DataBuffer

import scala.concurrent.Future

import scalaz.Kleisli

/**
  * Created by alonsodomin on 11/09/2016.
  */
final class AkkaHttpTransport private[http](host: String, port: Int = 80)(implicit val actorSystem: ActorSystem) extends HttpTransport {
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(actorSystem), "quckoo-http")

  val connection = Http().outgoingConnection(host, port)

  override def send: Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    def method: AkkaHttpMethod = req.method match {
      case HttpMethod.Get => HttpMethods.GET
      case HttpMethod.Put => HttpMethods.PUT
      case HttpMethod.Post => HttpMethods.POST
      case HttpMethod.Delete => HttpMethods.DELETE
    }

    def parseRawResponse(response: AkkaHttpResponse): Future[HttpResponse] = {
      val entityData = response.entity.dataBytes.
        map(bytes => DataBuffer(bytes.toByteBuffer)).
        runReduce(_ + _)

      import actorSystem.dispatcher
      entityData.map(buff => HttpResponse(response.status.intValue(), response.status.value, buff))
    }

    Source.single(AkkaHttpRequest(method, uri = req.url)).
      via(connection).
      mapAsync(1)(parseRawResponse).
      runWith(Sink.head[HttpResponse])
  }
}

object AkkaHttpTransport {

  def apply(host: String, port: Int = 80)(implicit actorSystem: ActorSystem): AkkaHttpTransport =
    new AkkaHttpTransport(host, port)

}