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

import org.scalatest._
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.concurrent.duration.Duration
import scala.concurrent.Future
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

class HttpProtocolSpec extends AsyncFlatSpec with Matchers with EitherValues with Inside {
  import HttpProtocolSpec._

  class TestClientRunner(client: TestClient) {
    def withClient(exec: TestClient => Future[Assertion]) = exec(client)
  }

  class HttpRequestStatement(matcher: Matcher[HttpRequest]) {
    def thenDo(process: HttpRequest => LawfulTry[HttpResponse]): TestClientRunner = {
      def handleRequest(req: HttpRequest): LawfulTry[HttpResponse] = {
        OutcomeOf.outcomeOf(req should matcher) match {
          case Succeeded => process(req)
          case Exceptional(ex) => throw ex
        }
      }
      val transport = new TestHttpTransport(handleRequest)
      new TestClientRunner(new TestClient(transport))
    }
  }

  def hasMethod(method: HttpMethod): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult =
      MatchResult(req.method == method,
        s"Http method ${req.method} is not a $method method",
        s"is a ${req.method} http request"
      )
  }

  def hasUrl(pattern: Regex): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult =
      MatchResult(pattern.findAllIn(req.url).nonEmpty,
        s"URL '${req.url}' does not match pattern '$pattern'",
        s"URL '${req.url}' matches pattern '$pattern'"
      )
  }

  def hasAuthHeader(username: String, password: String): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult = {
      val creds = DataBuffer.fromString(s"$username:$password").toBase64
      MatchResult(
        req.headers.get(AuthorizationHeader).contains(s"Basic $creds"),
        s"no '$AuthorizationHeader' header for '$username' with password '$password' was found in the request",
        s"'$AuthorizationHeader' header has the expected value"
      )
    }
  }

  def hasPassport(passport: Passport): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult = {
      MatchResult(
        req.headers.get(AuthorizationHeader).contains(s"Bearer ${passport.token}"),
        s"no '$AuthorizationHeader' header with passport '${passport.token}' was found in the request",
        s"'$AuthorizationHeader' header has the expected value"
      )
    }
  }

  def ensuringRequest(matcher: Matcher[HttpRequest]): HttpRequestStatement = {
    new HttpRequestStatement(matcher)
  }

  // -- Login requests

  def isLogin(username: String, password: String) =
    hasMethod(HttpMethod.Post) and hasUrl(uris.login) and hasAuthHeader(username, password)

  "authenticate" should "return the user's passport when result code is 200" in {
    val expectedPassport = new Passport(Map.empty, Map.empty, DataBuffer.fromString("foo"))

    ensuringRequest (isLogin("foo", "bar")) thenDo { _ =>
      HttpSuccess(DataBuffer.fromString(expectedPassport.token))
    } withClient { client =>
      client.authenticate("foo", "bar").map { passport =>
        passport shouldBe expectedPassport
      }
    }
  }

  it should "result in invalid credentials if result code is 401" in {
    ensuringRequest (isLogin("foo", "bar")) thenDo { _ =>
      HttpError(401, "TEST AUTH ERROR")
    } withClient { client =>
      recoverToSucceededIf[InvalidCredentialsException.type](client.authenticate("foo", "bar"))
    }
  }

  it should "result in an HTTP error if result code is not 401" in {
    ensuringRequest (isLogin("foo", "bar")) thenDo { _ =>
      HttpError(500, "TEST AUTH ERROR")
    } withClient { client =>
      recoverToSucceededIf[HttpErrorException](client.authenticate("foo", "bar"))
    }
  }

  // -- Logout requests

  val isLogout = hasMethod(HttpMethod.Post) and hasUrl(uris.logout) and hasPassport(TestPassport)

  "sing out" should "not return anything if it succeeds" in {
    ensuringRequest (isLogout) thenDo { _ =>
      HttpSuccess(DataBuffer.Empty)
    } withClient { client =>
      client.signOut.map(_ => succeed)
    }
  }

  it should "result in an HTTP error if result code is not 200" in {
    ensuringRequest (isLogout) thenDo {
      _ => HttpError(500, "TEST AUTH ERROR")
    } withClient { client =>
      recoverToSucceededIf[HttpErrorException](client.signOut)
    }
  }

  // -- Register Job

  val isRegisterJob = hasMethod(HttpMethod.Put) and hasUrl(uris.jobs) and hasPassport(TestPassport)

  "registerJob" should "return a validated JobId when it succeeds" in {
    ensuringRequest (isRegisterJob) thenDo { _ =>
      HttpSuccess(DataBuffer(TestJobId.successNel[Fault]))
    } withClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId.toEither.right.value shouldBe TestJobId
      }
    }
  }

  it should "return the missed dependencies when fails to resolve" in {
    val expectedFault = DownloadFailed(TestArtifactId, DownloadFailed.NotFound)
    ensuringRequest (isRegisterJob) thenDo {
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
    ensuringRequest (isFetchJobs) thenDo { _ =>
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
    ensuringRequest (isFetchJob) thenDo { req =>
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
    ensuringRequest (isFetchJob) thenDo { req =>
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

    ensuringRequest (isEnableJob) thenDo { req =>
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

    ensuringRequest (isEnableJob) thenDo { req =>
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

    ensuringRequest (isDisableJob) thenDo { req =>
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

    ensuringRequest (isDisableJob) thenDo { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobDisabled](JobId(id))))
    } withClient { client =>
      client.disableJob(TestJobId).map { res =>
        res shouldBe notFound[JobDisabled](TestJobId)
      }
    }
  }

}
