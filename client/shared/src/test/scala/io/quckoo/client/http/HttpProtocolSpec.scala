package io.quckoo.client.http

import java.util.UUID

import io.quckoo.JobSpec
import io.quckoo.auth.{InvalidCredentialsException, Passport}
import io.quckoo.client.QuckooClientV2
import io.quckoo.fault.{DownloadFailed, Fault}
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util._

import org.scalatest.{AsyncFlatSpec, EitherValues, Inside, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.Future

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

  class TestHttpTransport private[HttpProtocolSpec] (f: HttpRequest => LawfulTry[HttpResponse]) extends HttpTransport {
    def send: Kleisli[Future, HttpRequest, HttpResponse] =
      Kleisli(f).transform(lawfulTry2Future)
  }

  class TestClient private[HttpProtocolSpec] (transport: TestHttpTransport)
    extends QuckooClientV2[HttpProtocol](HttpDriver(transport))

  object HttpSuccess {
    def apply(entity: DataBuffer): LawfulTry[HttpResponse] = HttpResponse(200, "", entity).right[Throwable]
    def apply(tryEntity: LawfulTry[DataBuffer]): LawfulTry[HttpResponse] = tryEntity.flatMap(apply)
  }
  object HttpError {
    def apply(
      statusCode: Int,
      statusLine: String = "",
      entity: LawfulTry[DataBuffer] = DataBuffer.Empty.right[Throwable]
    ): LawfulTry[HttpResponse] =
      entity.map(data => HttpResponse(statusCode, statusLine, data))
  }

  def onRequest(f: HttpRequest => LawfulTry[HttpResponse]): TestClient = {
    val transport = new TestHttpTransport(f)
    new TestClient(transport)
  }

  def notFound[A](jobId: JobId): JobNotFound \/ A =
    JobNotFound(jobId).left[A]

  object uris {
    private[this] final val BaseURI = "/api"
    private[this] final val RegistryURI = s"$BaseURI/registry"

    val fetchJob = s"$RegistryURI/jobs/(.+)"
    val enableJob = s"$fetchJob/enable"
    val disableJob = s"$fetchJob/disable"
  }
}

class HttpProtocolSpec extends AsyncFlatSpec with Matchers with EitherValues with Inside {
  import HttpProtocolSpec._

  "authenticate" should "return the user's passport when result code is 200" in {
    val expectedPassport = new Passport(Map.empty, Map.empty, DataBuffer.fromString("foo"))
    val client = onRequest { _ =>
      HttpSuccess(DataBuffer.fromString(expectedPassport.token))
    }

    client.authenticate("foo", "bar").map { passport =>
      passport shouldBe expectedPassport
    }
  }

  it should "result in invalid credentials if result code is 401" in {
    val client = onRequest { _ => HttpError(401, "TEST AUTH ERROR") }

    recoverToSucceededIf[InvalidCredentialsException.type](client.authenticate("foo", "bar"))
  }

  it should "result in an HTTP error if result code is not 401" in {
    val client = onRequest { _ => HttpError(500, "TEST AUTH ERROR") }

    recoverToSucceededIf[HttpErrorException](client.authenticate("foo", "bar"))
  }

  "sing out" should "not return anything if it succeeds" in {
    val client = onRequest { _ => HttpSuccess(DataBuffer.Empty) }

    client.signOut.map(_ => succeed)
  }

  it should "result in an HTTP error if result code is not 200" in {
    val client = onRequest { _ => HttpError(500, "TEST AUTH ERROR") }

    recoverToSucceededIf[HttpErrorException](client.signOut)
  }

  "registerJob" should "return a validated JobId when it succeeds" in {
    val client = onRequest { _ => HttpSuccess(DataBuffer(TestJobId.successNel[Fault])) }

    client.registerJob(TestJobSpec).map { validatedJobId =>
      validatedJobId.toEither.right.value shouldBe TestJobId
    }
  }

  it should "return the missed dependencies when fails to resolve" in {
    val expectedFault = DownloadFailed(TestArtifactId, DownloadFailed.NotFound)
    val client = onRequest { _ => HttpSuccess(DataBuffer(expectedFault.failureNel[JobId])) }

    client.registerJob(TestJobSpec).map { validatedJobId =>
      validatedJobId.toEither.left.value shouldBe NonEmptyList(expectedFault)
    }
  }

  "fetchJob" should "return the job spec for a job ID" in {
    val urlPattern = uris.fetchJob.r
    val client = onRequest { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) {
        HttpSuccess(DataBuffer(TestJobSpec.some))
      } else {
        HttpError(500, s"Invalid URL: ${req.url}")
      }
    }

    client.fetchJob(TestJobId).map { jobSpec =>
      jobSpec shouldBe defined
      inside(jobSpec) { case Some(spec) =>
        spec shouldBe TestJobSpec
      }
    }
  }

  it should "return None if the job does not exist" in {
    val urlPattern = uris.fetchJob.r
    val client = onRequest { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) HttpError(404, "TEST 404")
      else HttpError(500, s"Invalid URL: ${req.url}")
    }

    client.fetchJob(TestJobId).map { jobSpec =>
      jobSpec should not be defined
    }
  }

  "enableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.enableJob.r

    def expectedResult(jobId: JobId) = JobEnabled(jobId).right[JobNotFound]
    val client = onRequest { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(expectedResult(JobId(id))))
    }

    client.enableJob(TestJobId).map { ack =>
      ack shouldBe expectedResult(TestJobId)
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.enableJob.r

    val client = onRequest { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobEnabled](JobId(id))))
    }

    client.enableJob(TestJobId).map { res =>
      res shouldBe notFound[JobEnabled](TestJobId)
    }
  }

  "disableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.disableJob.r

    def expectedResult(jobId: JobId) = JobDisabled(jobId).right[JobNotFound]
    val client = onRequest { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(expectedResult(JobId(id))))
    }

    client.disableJob(TestJobId).map { ack =>
      ack shouldBe expectedResult(TestJobId)
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.disableJob.r

    val client = onRequest { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobDisabled](JobId(id))))
    }

    client.disableJob(TestJobId).map { res =>
      res shouldBe notFound[JobDisabled](TestJobId)
    }
  }

}
