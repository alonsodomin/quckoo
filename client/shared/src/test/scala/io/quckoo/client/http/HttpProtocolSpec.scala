package io.quckoo.client.http

import java.util.UUID

import io.quckoo.JobSpec
import io.quckoo.auth.{InvalidCredentialsException, Passport}
import io.quckoo.client.QuckooClientV2
import io.quckoo.fault.{DownloadFailed, Fault}
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util._

import org.scalatest.{AsyncFlatSpec, EitherValues, Inside, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 15/09/2016.
  */
object HttpProtocolSpec {
  implicit final val TestPassport = new Passport(Map.empty, Map.empty, DataBuffer.fromString("foo"))
  implicit final val TestDuration = Duration.Inf

  final val TestArtifactId = ArtifactId("com.example", "bar", "latest")
  final val TestJobId = JobId(UUID.randomUUID())
  final val TestJobSpec = JobSpec("foo",
    artifactId = TestArtifactId,
    jobClass = "com.example.Job"
  )
}

class HttpProtocolSpec extends AsyncFlatSpec with Matchers with EitherValues with Inside {
  import HttpProtocolSpec._

  class TestHttpTransport(f: HttpRequest => TryE[HttpResponse]) extends HttpTransport {
    def send(implicit ec: ExecutionContext): Kleisli[Future, HttpRequest, HttpResponse] =
      Kleisli(f).transform(either2Future)
  }

  class TestClient(transport: TestHttpTransport) extends QuckooClientV2[HttpProtocol](HttpDriver(transport))

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

  "registerJob" should "return a validated JobId when it succeeds" in {
    val transport = new TestHttpTransport(_ => DataBuffer(TestJobId.successNel[Fault]).map(HttpSuccess))
    val client = new TestClient(transport)

    client.registerJob(TestJobSpec).map { validatedJobId =>
      validatedJobId.toEither.right.value shouldBe TestJobId
    }
  }

  it should "return the missed dependencies when fails to resolve" in {
    val expectedFault = DownloadFailed(TestArtifactId, DownloadFailed.NotFound)
    val transport = new TestHttpTransport(_ => DataBuffer(expectedFault.failureNel[JobId]).map(HttpSuccess))
    val client = new TestClient(transport)

    client.registerJob(TestJobSpec).map { validatedJobId =>
      validatedJobId.toEither.left.value shouldBe NonEmptyList(expectedFault)
    }
  }

  "fetchJob" should "return the job spec for a job ID" in {
    val transport = new TestHttpTransport(_ => DataBuffer(TestJobSpec.some).map(HttpSuccess))
    val client = new TestClient(transport)

    client.fetchJob(TestJobId).map { jobSpec =>
      jobSpec shouldBe defined
      inside(jobSpec) { case Some(spec) =>
        spec shouldBe TestJobSpec
      }
    }
  }

  it should "return None if the job does not exist" in {
    val transport = new TestHttpTransport(_ => HttpError(404, "TEST 404").right[Throwable])
    val client = new TestClient(transport)

    client.fetchJob(TestJobId).map { jobSpec =>
      jobSpec should not be defined
    }
  }

}
