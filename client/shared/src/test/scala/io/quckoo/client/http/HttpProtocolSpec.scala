package io.quckoo.client.http

import java.util.UUID

import io.quckoo.JobSpec
import io.quckoo.auth.{InvalidCredentialsException, Passport}
import io.quckoo.client.core.StubClient
import io.quckoo.fault.{DownloadFailed, Fault}
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util._

import org.scalatest._

import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.util.matching.Regex

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

  def notFound[A](jobId: JobId): JobNotFound \/ A =
    JobNotFound(jobId).left[A]

  object uris {
    private[this] final val BaseURI = "/api"
    private[this] final val RegistryURI = s"$BaseURI/registry"

    val login = s"$BaseURI/auth/login"
    val logout = s"$BaseURI/auth/logout"

    val jobs = s"$RegistryURI/jobs"
    val fetchJob = s"$jobs/(.+)"
    val enableJob = s"$fetchJob/enable"
    val disableJob = s"$fetchJob/disable"
  }

  implicit def string2Regex(str: String): Regex = str.r

}

class HttpProtocolSpec extends AsyncFlatSpec with HttpRequestMatchers with StubClient with EitherValues with Inside {
  import HttpProtocolSpec._

  // -- Login requests

  def isLogin(username: String, password: String) =
    hasMethod(HttpMethod.Post) and
      hasUrl(uris.login) and
      hasAuthHeader(username, password) and
      not(matcher = hasPassport(TestPassport))

  "authenticate" should "return the user's passport when result code is 200" in {
    val expectedPassport = new Passport(Map.empty, Map.empty, DataBuffer.fromString("foo"))

    inProtocol(HttpProtocol) ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpSuccess(DataBuffer.fromString(expectedPassport.token))
    } withClient { client =>
      client.authenticate("foo", "bar").map { passport =>
        passport shouldBe expectedPassport
      }
    }
  }

  it should "result in invalid credentials if result code is 401" in {
    inProtocol(HttpProtocol) ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpError(401, "TEST AUTH ERROR")
    } withClient { client =>
      recoverToSucceededIf[InvalidCredentialsException.type](client.authenticate("foo", "bar"))
    }
  }

  it should "result in an HTTP error if result code is not 401" in {
    inProtocol(HttpProtocol) ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpError(500, "TEST AUTH ERROR")
    } withClient { client =>
      recoverToSucceededIf[HttpErrorException](client.authenticate("foo", "bar"))
    }
  }

  // -- Logout requests

  val isLogout = hasMethod(HttpMethod.Post) and hasUrl(uris.logout) and hasPassport(TestPassport)

  "sing out" should "not return anything if it succeeds" in {
    inProtocol(HttpProtocol) ensuringRequest isLogout replyWith { _ =>
      HttpSuccess(DataBuffer.Empty)
    } withClient { client =>
      client.signOut.map(_ => succeed)
    }
  }

  it should "result in an HTTP error if result code is not 200" in {
    inProtocol(HttpProtocol) ensuringRequest isLogout replyWith {
      _ => HttpError(500, "TEST AUTH ERROR")
    } withClient { client =>
      recoverToSucceededIf[HttpErrorException](client.signOut)
    }
  }

  // -- Register Job

  val isRegisterJob = hasMethod(HttpMethod.Put) and hasUrl(uris.jobs) and hasPassport(TestPassport)

  "registerJob" should "return a validated JobId when it succeeds" in {
    inProtocol(HttpProtocol) ensuringRequest isRegisterJob replyWith { _ =>
      HttpSuccess(DataBuffer(TestJobId.successNel[Fault]))
    } withClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId.toEither.right.value shouldBe TestJobId
      }
    }
  }

  it should "return the missed dependencies when fails to resolve" in {
    val expectedFault = DownloadFailed(TestArtifactId, DownloadFailed.NotFound)
    inProtocol(HttpProtocol) ensuringRequest isRegisterJob replyWith {
      _ => HttpSuccess(DataBuffer(expectedFault.failureNel[JobId]))
    } withClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId.toEither.left.value shouldBe NonEmptyList(expectedFault)
      }
    }
  }

  // -- Fetch Jobs

  val isFetchJobs = hasMethod(HttpMethod.Get) and hasUrl(uris.jobs) and hasPassport(TestPassport)

  "fetchJobs" should "return a map of the job specs" in {
    inProtocol(HttpProtocol) ensuringRequest isFetchJobs replyWith { _ =>
      HttpSuccess(DataBuffer(Map(TestJobId -> TestJobSpec)))
    } withClient { client =>
      client.fetchJobs.map { jobMap =>
        jobMap should contain key TestJobId
        jobMap should contain value TestJobSpec
      }
    }
  }

  // -- Fetch Job

  val isFetchJob = hasMethod(HttpMethod.Get) and hasUrl(uris.fetchJob) and hasPassport(TestPassport)

  "fetchJob" should "return the job spec for a job ID" in {
    val urlPattern = uris.fetchJob.r
    inProtocol(HttpProtocol) ensuringRequest isFetchJob replyWith { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) {
        HttpSuccess(DataBuffer(TestJobSpec.some))
      } else {
        HttpError(500, s"Invalid JobId: $id")
      }
    } withClient { client =>
      client.fetchJob(TestJobId).map { jobSpec =>
        jobSpec shouldBe defined
        inside(jobSpec) { case Some(spec) =>
          spec shouldBe TestJobSpec
        }
      }
    }
  }

  it should "return None if the job does not exist" in {
    val urlPattern = uris.fetchJob.r
    inProtocol(HttpProtocol) ensuringRequest isFetchJob replyWith { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) HttpError(404, "TEST 404")
      else HttpError(500, s"Invalid URL: ${req.url}")
    } withClient { client =>
      client.fetchJob(TestJobId).map { jobSpec =>
        jobSpec should not be defined
      }
    }
  }

  // -- Enable Job

  val isEnableJob = hasMethod(HttpMethod.Post) and hasUrl(uris.enableJob) and hasPassport(TestPassport)

  "enableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.enableJob.r
    def expectedResult(jobId: JobId) = JobEnabled(jobId).right[JobNotFound]

    inProtocol(HttpProtocol) ensuringRequest isEnableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(expectedResult(JobId(id))))
    } withClient { client =>
      client.enableJob(TestJobId).map { ack =>
        ack shouldBe expectedResult(TestJobId)
      }
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.enableJob.r

    inProtocol(HttpProtocol) ensuringRequest isEnableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobEnabled](JobId(id))))
    } withClient { client =>
      client.enableJob(TestJobId).map { res =>
        res shouldBe notFound[JobEnabled](TestJobId)
      }
    }
  }

  // -- Disable Job

  val isDisableJob = hasMethod(HttpMethod.Post) and hasUrl(uris.disableJob) and hasPassport(TestPassport)

  "disableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.disableJob.r
    def expectedResult(jobId: JobId) = JobDisabled(jobId).right[JobNotFound]

    inProtocol(HttpProtocol) ensuringRequest isDisableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(expectedResult(JobId(id))))
    } withClient { client =>
      client.disableJob(TestJobId).map { ack =>
        ack shouldBe expectedResult(TestJobId)
      }
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.disableJob.r

    inProtocol(HttpProtocol) ensuringRequest isDisableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobDisabled](JobId(id))))
    } withClient { client =>
      client.disableJob(TestJobId).map { res =>
        res shouldBe notFound[JobDisabled](TestJobId)
      }
    }
  }

}
