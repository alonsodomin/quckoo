package io.quckoo.client.http

import io.quckoo.auth.http._
import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.serialization.Base64
import io.quckoo.client.core._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Failure => Fail}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 10/09/2016.
  */
class HttpDriver(protected val transport: HttpTransport, baseUrl: String = "")
  extends Driver[Protocol.Http] {
  type TransportRepr = HttpTransport

  val encodings = new Encodings {
    implicit val credentialsEnc: Encoding[Credentials, Passport] = new Encoding[Credentials, Passport] {
      override val encode: Encoder[Credentials, HttpRequest] = { cmd =>
        import Base64._

        Try(s"${cmd.payload.username}:${cmd.payload.password}".getBytes("UTF-8").toBase64).map { creds =>
          val hdrs = Map(AuthorizationHeader -> s"Basic $creds")
          HttpRequest(HttpMethod.Post, LoginURI, cmd.timeout, headers = hdrs, None)
        }
      }

      override val decode: Decoder[HttpResponse, Passport] = {
        case HttpSuccess(payload) =>
          Try(new Passport(new String(payload.array(), "UTF-8")))

        case err: HttpError =>
          Fail(HttpErrorException(err))
      }
    }
  }

  override def invoke[In, Out](implicit
    ec: ExecutionContext,
    encoding: Encoding[In, Out]
  ): Kleisli[Future, Command[In], Out] = {
    val encodeRequest  = Kleisli(encoding.encode).transform(try2Future)
    val decodeResponse = Kleisli(encoding.decode).transform(try2Future)

    encodeRequest >=> transport.send >=> decodeResponse
  }
}
