package io.quckoo.client.http

import java.util.UUID

import io.quckoo._
import io.quckoo.auth.{InvalidCredentialsException, Passport}
import io.quckoo.client.core.StubClient
import io.quckoo.fault.{DownloadFailed, Fault}
import io.quckoo.id.{ArtifactId, JobId, PlanId, TaskId}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.cluster.{MasterEvent, MasterReachable, MasterRemoved}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util._

import monix.execution.Scheduler

import org.threeten.bp._
import org.scalatest._

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.matching.Regex

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 15/09/2016.
  */
object HttpProtocolSpec {
  final val FixedInstant = Instant.ofEpochMilli(903029302L)
  implicit final val FixedClock = Clock.fixed(FixedInstant, ZoneId.of("UTC"))

  def generatePassport(): Passport = {
    val header = DataBuffer.fromString("{}").toBase64
    val claims = DataBuffer.fromString("{}").toBase64
    val signature = DataBuffer.fromString(System.currentTimeMillis().toString).toBase64
    new Passport(Map.empty, s"$header.$claims.$signature")
  }

  implicit final val TestPassport = generatePassport()
  implicit final val TestDuration = Duration.Inf

  final val TestArtifactId = ArtifactId("com.example", "bar", "latest")
  final val TestJobId = JobId(UUID.randomUUID())
  final val TestJobSpec = JobSpec("foo",
    artifactId = TestArtifactId,
    jobClass = "com.example.Job"
  )

  final val TestPlanId: PlanId = UUID.randomUUID()
  final val TestExecutionPlan = ExecutionPlan(
    TestJobId, TestPlanId, Trigger.Immediate, ZonedDateTime.now(FixedClock)
  )

  final val TestTaskId: TaskId = UUID.randomUUID()
  final val TestTaskExecution = TaskExecution(
    TestPlanId, Task(TestTaskId, TestArtifactId, "com.example.Task"), TaskExecution.Complete
  )

  object HttpSuccess {
    def apply(entity: DataBuffer): LawfulTry[HttpResponse] =
      LawfulTry.success(HttpResponse(200, "", entity))

    def apply(tryEntity: LawfulTry[DataBuffer]): LawfulTry[HttpResponse] = tryEntity.flatMap(apply)
  }
  object HttpError {
    def apply(
      statusCode: Int,
      statusLine: String = "",
      entity: LawfulTry[DataBuffer] = LawfulTry.success(DataBuffer.Empty)
    ): LawfulTry[HttpResponse] =
      entity.map(data => HttpResponse(statusCode, statusLine, data))
  }

  def notFound[A](jobId: JobId): JobNotFound \/ A =
    JobNotFound(jobId).left[A]

  object uris {
    private[this] final val BaseURI = "/api"
    private[this] final val AuthURI = s"$BaseURI/auth"
    private[this] final val RegistryURI = s"$BaseURI/registry"
    private[this] final val SchedulerURI = s"$BaseURI/scheduler"

    val login = s"$AuthURI/login"
    val refreshPass = s"$AuthURI/refresh"
    val logout = s"$AuthURI/logout"

    val cluster = s"$BaseURI/cluster"

    val jobs = s"$RegistryURI/jobs"
    val fetchJob = s"$jobs/(.+)"
    val enableJob = s"$fetchJob/enable"
    val disableJob = s"$fetchJob/disable"

    val executionPlans = s"$SchedulerURI/plans"
    val executionPlan = s"$executionPlans/(.+)"
    val executions = s"$SchedulerURI/executions"
    val execution = s"$executions/(.+)"
  }

  implicit def string2Regex(str: String): Regex = str.r

}

class HttpProtocolSpec extends AsyncFlatSpec with HttpRequestMatchers with StubClient with EitherValues with Inside {
  import HttpProtocolSpec._

  implicit val scheduler: Scheduler = Scheduler.global
  override implicit def executionContext: ExecutionContext = scheduler

  // -- Subscribe

  "subscribe" should "return an stream of events" in {
    val givenEvents = List(MasterReachable(UUID.randomUUID()))

    val httpEvents: LawfulTry[List[HttpServerSentEvent]] = EitherT(givenEvents.map(evt => DataBuffer(evt))).
      map(HttpServerSentEvent(_)).
      run.sequenceU

    lawfulTry2Future(httpEvents).flatMap { events =>
      inProtocol[HttpProtocol] withEvents events usingClient { client =>
        val collectEvents = client.channel[MasterEvent].toListL
        collectEvents.runAsync.map { evts =>
          evts shouldBe givenEvents
        }
      }
    }
  }

  // -- Login requests

  def isLogin(username: String, password: String) =
    hasMethod(HttpMethod.Post) and
      hasUrl(uris.login) and
      hasEmptyBody and
      hasAuth(username, password) and
      not(matcher = hasPassport(TestPassport))

  "authenticate" should "return the user's passport when result code is 200" in {
    val expectedPassport = generatePassport()

    inProtocol[HttpProtocol] ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpSuccess(DataBuffer.fromString(expectedPassport.token))
    } usingClient { client =>
      client.authenticate("foo", "bar").map { passport =>
        passport shouldBe expectedPassport
      }
    }
  }

  it should "result in invalid credentials if result code is 401" in {
    inProtocol[HttpProtocol] ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpError(401, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[InvalidCredentialsException.type](client.authenticate("foo", "bar"))
    }
  }

  it should "result in an HTTP error if result code is not 401" in {
    inProtocol[HttpProtocol] ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpError(500, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[HttpErrorException](client.authenticate("foo", "bar"))
    }
  }

  // -- Refresh token requests

  val isRefreshPassport = hasMethod(HttpMethod.Post) and
    hasUrl(uris.refreshPass) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "refreshPassport" should "return a new passport" in {
    val expectedPassport = generatePassport()

    inProtocol[HttpProtocol] ensuringRequest isRefreshPassport replyWith { _ =>
      HttpSuccess(DataBuffer.fromString(expectedPassport.token))
    } usingClient { client =>
      client.refreshPassport.map { returnedPass =>
        returnedPass shouldBe expectedPassport
      }
    }
  }

  it should "result in invalid credentials if result code is 401" in {
    inProtocol[HttpProtocol] ensuringRequest isRefreshPassport replyWith { _ =>
      HttpError(401, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[InvalidCredentialsException.type](client.refreshPassport)
    }
  }

  it should "result in an HTTP error if result code is not 401" in {
    inProtocol[HttpProtocol] ensuringRequest isRefreshPassport replyWith { _ =>
      HttpError(500, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[HttpErrorException](client.refreshPassport)
    }
  }

  // -- Logout requests

  val isLogout = hasMethod(HttpMethod.Post) and
    hasUrl(uris.logout) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "sing out" should "not return anything if it succeeds" in {
    inProtocol[HttpProtocol] ensuringRequest isLogout replyWith { _ =>
      HttpSuccess(DataBuffer.Empty)
    } usingClient { client =>
      client.signOut.map(_ => succeed)
    }
  }

  it should "result in an HTTP error if result code is not 200" in {
    inProtocol[HttpProtocol] ensuringRequest isLogout replyWith {
      _ => HttpError(500, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[HttpErrorException](client.signOut)
    }
  }

  // -- Get Cluster State

  val isGetClusterState = hasMethod(HttpMethod.Get) and
    hasUrl(uris.cluster) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "clusterState" should "return the cluster state details" in {
    val expectedState = QuckooState()
    inProtocol[HttpProtocol] ensuringRequest isGetClusterState replyWith { req =>
      HttpSuccess(DataBuffer(expectedState))
    } usingClient { client =>
      client.clusterState.map { returnedState =>
        returnedState shouldBe expectedState
      }
    }
  }

  // -- Register Job

  val isRegisterJob = hasMethod(HttpMethod.Put) and
    hasUrl(uris.jobs) and
    hasPassport(TestPassport) and
    hasBody(RegisterJob(TestJobSpec)) and
    isJsonRequest

  "registerJob" should "return a validated JobId when it succeeds" in {
    inProtocol[HttpProtocol] ensuringRequest isRegisterJob replyWith { _ =>
      HttpSuccess(DataBuffer(TestJobId.successNel[Fault]))
    } usingClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId.toEither.right.value shouldBe TestJobId
      }
    }
  }

  it should "return the missed dependencies when fails to resolve" in {
    val expectedFault = DownloadFailed(TestArtifactId, DownloadFailed.NotFound)
    inProtocol[HttpProtocol] ensuringRequest isRegisterJob replyWith {
      _ => HttpSuccess(DataBuffer(expectedFault.failureNel[JobId]))
    } usingClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId.toEither.left.value shouldBe NonEmptyList(expectedFault)
      }
    }
  }

  // -- Fetch Jobs

  val isFetchJobs = hasMethod(HttpMethod.Get) and
    hasUrl(uris.jobs) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "fetchJobs" should "return a map of the job specs" in {
    inProtocol[HttpProtocol] ensuringRequest isFetchJobs replyWith { _ =>
      HttpSuccess(DataBuffer(Map(TestJobId -> TestJobSpec)))
    } usingClient { client =>
      client.fetchJobs.map { jobMap =>
        jobMap should contain key TestJobId
        jobMap should contain value TestJobSpec
      }
    }
  }

  // -- Fetch Job

  val isFetchJob = hasMethod(HttpMethod.Get) and
    hasUrl(uris.fetchJob) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "fetchJob" should "return the job spec for a job ID" in {
    val urlPattern = uris.fetchJob.r
    inProtocol[HttpProtocol] ensuringRequest isFetchJob replyWith { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) {
        HttpSuccess(DataBuffer(TestJobSpec.some))
      } else {
        HttpError(500, s"Invalid JobId: $id")
      }
    } usingClient { client =>
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
    inProtocol[HttpProtocol] ensuringRequest isFetchJob replyWith { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) HttpError(404, "TEST 404", DataBuffer(none[JobSpec]))
      else HttpError(500, s"Invalid URL: ${req.url}")
    } usingClient { client =>
      client.fetchJob(TestJobId).map { jobSpec =>
        jobSpec should not be defined
      }
    }
  }

  // -- Enable Job

  val isEnableJob = hasMethod(HttpMethod.Post) and
    hasUrl(uris.enableJob) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "enableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.enableJob.r
    def expectedResult(jobId: JobId) = JobEnabled(jobId).right[JobNotFound]

    inProtocol[HttpProtocol] ensuringRequest isEnableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(expectedResult(JobId(id))))
    } usingClient { client =>
      client.enableJob(TestJobId).map { ack =>
        ack shouldBe expectedResult(TestJobId)
      }
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.enableJob.r

    inProtocol[HttpProtocol] ensuringRequest isEnableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobEnabled](JobId(id))))
    } usingClient { client =>
      client.enableJob(TestJobId).map { res =>
        res shouldBe notFound[JobEnabled](TestJobId)
      }
    }
  }

  // -- Disable Job

  val isDisableJob = hasMethod(HttpMethod.Post) and
    hasUrl(uris.disableJob) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "disableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.disableJob.r
    def expectedResult(jobId: JobId) = JobDisabled(jobId).right[JobNotFound]

    inProtocol[HttpProtocol] ensuringRequest isDisableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(expectedResult(JobId(id))))
    } usingClient { client =>
      client.disableJob(TestJobId).map { ack =>
        ack shouldBe expectedResult(TestJobId)
      }
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.disableJob.r

    inProtocol[HttpProtocol] ensuringRequest isDisableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(notFound[JobDisabled](JobId(id))))
    } usingClient { client =>
      client.disableJob(TestJobId).map { res =>
        res shouldBe notFound[JobDisabled](TestJobId)
      }
    }
  }

  // -- Get execution plans

  val isGetExecutionPlans = hasMethod(HttpMethod.Get) and
    hasUrl(uris.executionPlans) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "executionPlans" should "return a map with current exection plans" in {
    inProtocol[HttpProtocol] ensuringRequest isGetExecutionPlans replyWith {
      _ => HttpSuccess(DataBuffer(Map(TestPlanId -> TestExecutionPlan)))
    } usingClient { client =>
      client.executionPlans.map { returnedPlans =>
        returnedPlans should contain key TestPlanId
        returnedPlans should contain value TestExecutionPlan
      }
    }
  }

  // -- Get execution plan

  val isGetExecutionPlan = hasMethod(HttpMethod.Get) and
    hasUrl(uris.executionPlan) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "executionPlan" should "return an execution plan for a given ID" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isGetExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (UUID.fromString(id) == TestPlanId) {
        HttpSuccess(DataBuffer(TestExecutionPlan.some))
      } else {
        HttpError(500, s"Invalid plan id $id")
      }
    } usingClient { client =>
      client.executionPlan(TestPlanId).map { returnedPlan =>
        returnedPlan shouldBe TestExecutionPlan.some
      }
    }
  }

  it should "return None if the HTTP status code is 404" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isGetExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (UUID.fromString(id) == TestPlanId) {
        HttpError(404, s"Should have returned a None instance", DataBuffer(none[ExecutionPlan]))
      } else {
        HttpError(500, s"Invalid plan id $id")
      }
    } usingClient { client =>
      client.executionPlan(TestPlanId).map { returnedPlan =>
        returnedPlan should not be defined
      }
    }
  }

  // -- Schedule job

  def isScheduleJob(value: ScheduleJob) = hasMethod(HttpMethod.Put) and
    hasUrl(uris.executionPlans) and
    hasPassport(TestPassport) and
    hasBody(value) and
    isJsonRequest

  "scheduleJob" should "return the started notification" in {
    val payload = ScheduleJob(TestJobId)
    inProtocol[HttpProtocol] ensuringRequest isScheduleJob(payload) replyWith { req =>
      val result = req.entity.as[ScheduleJob].
        flatMap(cmd => DataBuffer(ExecutionPlanStarted(cmd.jobId, TestPlanId).right[JobNotFound]))
      HttpSuccess(result)
    } usingClient { client =>
      client.scheduleJob(payload).map { returned =>
        returned shouldBe ExecutionPlanStarted(TestJobId, TestPlanId).right[JobNotFound]
      }
    }
  }

  it should "return JobNotFound if the HTTP status code is 404" in {
    val payload = ScheduleJob(TestJobId)
    inProtocol[HttpProtocol] ensuringRequest isScheduleJob(payload) replyWith { req =>
      val result = req.entity.as[ScheduleJob].
        flatMap(cmd => DataBuffer(JobNotFound(cmd.jobId).left[ExecutionPlanStarted]))
      HttpError(404, entity = result)
    } usingClient { client =>
      client.scheduleJob(payload).map { returned =>
        returned shouldBe JobNotFound(TestJobId).left[ExecutionPlanStarted]
      }
    }
  }

  // -- Cancel execution plan

  val isCancelExecutionPlan = hasMethod(HttpMethod.Delete) and
    hasUrl(uris.executionPlan) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "cancelExecutionPlan" should "return nothing if the plan has been cancelled" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isCancelExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (UUID.fromString(id) == TestPlanId) {
        HttpSuccess(DataBuffer(().right[ExecutionPlanNotFound]))
      } else {
        HttpError(500, s"Invalid plan id $id")
      }
    } usingClient { client =>
      client.cancelExecutionPlan(TestPlanId).map { returned =>
        returned.isRight shouldBe true
      }
    }
  }

  it should "return not found if the HTTP status code is 404" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isCancelExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (UUID.fromString(id) == TestPlanId) {
        HttpError(404, entity = DataBuffer(ExecutionPlanNotFound(TestPlanId).left[Unit]))
      } else {
        HttpError(500, s"Invalid plan id $id")
      }
    } usingClient { client =>
      client.cancelExecutionPlan(TestPlanId).map { returned =>
        returned shouldBe ExecutionPlanNotFound(TestPlanId).left[Unit]
      }
    }
  }

  // -- Get executions

  val isGetExecutions = hasMethod(HttpMethod.Get) and
    hasUrl(uris.executions) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "executions" should "return a map with the executions" in {
    inProtocol[HttpProtocol] ensuringRequest isGetExecutions replyWith {
      _ => HttpSuccess(DataBuffer(Map(TestTaskId -> TestTaskExecution)))
    } usingClient { client =>
      client.executions.map { returnedExecutions =>
        returnedExecutions should contain key TestTaskId
        returnedExecutions should contain value TestTaskExecution
      }
    }
  }

  // -- Get execution

  val isGetExecution = hasMethod(HttpMethod.Get) and
    hasUrl(uris.execution) and
    hasPassport(TestPassport) and
    hasEmptyBody

  "execution" should "return an execution for a given ID" in {
    val urlPattern = uris.execution.r
    inProtocol[HttpProtocol] ensuringRequest isGetExecution replyWith { req =>
      val urlPattern(id) = req.url
      if (UUID.fromString(id) == TestTaskId) {
        HttpSuccess(DataBuffer(TestTaskExecution.some))
      } else {
        HttpError(500, s"Invalid task id $id")
      }
    } usingClient { client =>
      client.execution(TestTaskId).map { returnedExec =>
        returnedExec shouldBe TestTaskExecution.some
      }
    }
  }

  it should "return None if the HTTP status code is 404" in {
    val urlPattern = uris.execution.r
    inProtocol[HttpProtocol] ensuringRequest isGetExecution replyWith { req =>
      val urlPattern(id) = req.url
      if (UUID.fromString(id) == TestTaskId) {
        HttpError(404, s"Should have returned a None instance", DataBuffer(none[TaskExecution]))
      } else {
        HttpError(500, s"Invalid task id $id")
      }
    } usingClient { client =>
      client.execution(TestTaskId).map { returnedExec =>
        returnedExec should not be defined
      }
    }
  }

}
