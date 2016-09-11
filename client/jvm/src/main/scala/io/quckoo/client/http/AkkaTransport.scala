package io.quckoo.client.http

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpMethod => AkkaHttpMethod, HttpRequest => AkkaHttpRequest, HttpResponse => AkkaHttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Kleisli

/**
  * Created by alonsodomin on 11/09/2016.
  */
private[http] class AkkaTransport(host: String, port: Int = 80)(implicit val actorSystem: ActorSystem) extends HttpTransport {
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(actorSystem), "quckoo-http")

  override def send(implicit ec: ExecutionContext): Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    def method: AkkaHttpMethod = req.method match {
      case HttpMethod.Get => HttpMethods.GET
      case HttpMethod.Put => HttpMethods.PUT
      case HttpMethod.Post => HttpMethods.POST
      case HttpMethod.Delete => HttpMethods.DELETE
    }

    val connection = Http().outgoingConnection(host, port)
    Source.single(AkkaHttpRequest(method, uri = req.url)).
      via(connection).
      map { res =>
        if (res.status.isFailure()) HttpError(res.status.intValue(), res.status.value)
        else HttpSuccess(ByteBuffer.allocate(0))
      }.runWith(Sink.head[HttpResponse])
  }
}
