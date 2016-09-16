package io.quckoo.client.http

import io.quckoo.auth.{InvalidCredentialsException, Passport}
import io.quckoo.client.QuckooClientV2
import io.quckoo.client.core.Protocol
import io.quckoo.serialization.DataBuffer
import io.quckoo.util._

import org.scalatest.{AsyncFlatSpec, FutureOutcome, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 15/09/2016.
  */
object HttpDriverSpec {
  implicit final val TestPassport = new Passport(Map.empty, Map.empty, DataBuffer.fromString("foo"))
  implicit final val TestDuration = Duration.Inf
}

class HttpDriverSpec extends AsyncFlatSpec with Matchers {
  import HttpDriverSpec._

  class TestHttpTransport(f: HttpRequest => TryE[HttpResponse]) extends HttpTransport {
    def send(implicit ec: ExecutionContext): Kleisli[Future, HttpRequest, HttpResponse] =
      Kleisli(f).transform(either2Future)
  }

  class TestClient(transport: TestHttpTransport) extends QuckooClientV2[Protocol.Http](new HttpDriver(transport))

  "authenticate" should "return the user's passport when result code is 200" in {
    val expectedPassport = new Passport(Map.empty, Map.empty, DataBuffer.fromString("foo"))
    val transport = new TestHttpTransport({ _ =>
      HttpSuccess(DataBuffer.fromString(expectedPassport.token)).right[Throwable]
    })
    val client = new TestClient(transport)

    client.authenticate("foo", "bar").map { passport =>
      passport shouldBe expectedPassport
    }
  }

  it should "result in invalid credentials if result code is 401" in {
    val transport = new TestHttpTransport({ _ => HttpError(401, "TEST AUTH ERROR").right[Throwable] })
    val client = new TestClient(transport)

    recoverToSucceededIf[InvalidCredentialsException.type](client.authenticate("foo", "bar"))
  }

  it should "result in an HTTP error if result code is not 401" in {
    val transport = new TestHttpTransport({ _ => HttpError(500, "TEST AUTH ERROR").right[Throwable] })
    val client = new TestClient(transport)

    recoverToSucceededIf[HttpErrorException](client.authenticate("foo", "bar"))
  }

  "sing out" should "not return anything if it succeeds" in {
    val transport = new TestHttpTransport(_ => HttpSuccess(DataBuffer.Empty).right[Throwable])
    val client = new TestClient(transport)

    client.signOut.map(_ => succeed)
  }

  it should "result in an HTTP error if result code is not 200" in {
    val transport = new TestHttpTransport({ _ => HttpError(500, "TEST AUTH ERROR").right[Throwable] })
    val client = new TestClient(transport)

    recoverToSucceededIf[HttpErrorException](client.signOut)
  }

}
