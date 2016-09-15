package io.quckoo.client.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpMethod => AkkaHttpMethod, HttpRequest => AkkaHttpRequest, HttpResponse => AkkaHttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import io.quckoo.serialization.DataBuffer

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 11/09/2016.
  */
private[http] class AkkaTransport(host: String, port: Int = 80)(implicit val actorSystem: ActorSystem) extends HttpTransport {
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(actorSystem), "quckoo-http")

  val connection = Http().outgoingConnection(host, port)

  override def send(implicit ec: ExecutionContext): Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    def method: AkkaHttpMethod = req.method match {
      case HttpMethod.Get => HttpMethods.GET
      case HttpMethod.Put => HttpMethods.PUT
      case HttpMethod.Post => HttpMethods.POST
      case HttpMethod.Delete => HttpMethods.DELETE
    }

    def collectEntityData(response: AkkaHttpResponse): Future[DataBuffer] = {
      response.entity.dataBytes.
        map(bytes => DataBuffer(bytes.toByteBuffer)).
        runReduce(_ + _)
    }

    Source.single(AkkaHttpRequest(method, uri = req.url)).
      via(connection).
      mapAsync(1) { res =>
        if (res.status.isFailure()) Future.successful(HttpError(res.status.intValue(), res.status.value))
        else collectEntityData(res).map(HttpSuccess)
      }.runWith(Sink.head[HttpResponse])
  }
}
