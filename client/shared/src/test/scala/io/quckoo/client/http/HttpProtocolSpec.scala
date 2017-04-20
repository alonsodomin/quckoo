/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.client.http

import java.util.UUID
import java.time._

import cats.data.{EitherT, NonEmptyList, Validated}
import cats.implicits._

import io.circe.generic.auto._

import io.quckoo._
import io.quckoo.auth.{InvalidCredentials, Passport}
import io.quckoo.client.core.StubClient
import io.quckoo.net.{QuckooState, Location}
import io.quckoo.protocol.cluster.{MasterEvent, MasterJoined, MasterReachable}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.testkit.ImplicitClock
import io.quckoo.util._

import monix.execution.Scheduler

import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.matching.Regex

/**
  * Created by alonsodomin on 15/09/2016.
  */
object HttpProtocolSpec {
  final val FixedInstant = Instant.ofEpochMilli(903029302L)
  implicit final val FixedClock = Clock.fixed(FixedInstant, ZoneId.of("UTC"))

  def generatePassport(): Passport = {
    val Right(passport) = for {
      header    <- DataBuffer.fromString("{}").toBase64
      claims    <- DataBuffer.fromString("{}").toBase64
      signature <- DataBuffer.fromString(System.currentTimeMillis().toString).toBase64
    } yield new Passport(Map.empty, s"$header.$claims.$signature")

    passport
  }

  implicit final val TestPassport = generatePassport()
  implicit final val TestTimeout = 10 seconds

  final val TestArtifactId = ArtifactId("com.example", "bar", "latest")
  final val TestJobId = JobId("fooId")
  final val TestJobSpec = JobSpec("foo", jobPackage = JobPackage.jar(
    artifactId = TestArtifactId,
    jobClass = "com.example.Job"
  ))

  final val TestPlanId: PlanId = PlanId(UUID.randomUUID())
  final val TestExecutionPlan = ExecutionPlan(
    TestJobId, TestPlanId, Trigger.Immediate, ZonedDateTime.now(FixedClock)
  )

  final val TestTaskId: TaskId = TaskId(UUID.randomUUID())
  final val TestTaskExecution = TaskExecution(
    TestPlanId, Task(TestTaskId, TestJobSpec.jobPackage), TaskExecution.Status.Complete
  )

  object HttpSuccess {
    def apply(entity: DataBuffer): Attempt[HttpResponse] =
      Attempt.success(HttpResponse(200, "", entity))

    def apply(tryEntity: Attempt[DataBuffer]): Attempt[HttpResponse] = tryEntity.flatMap(apply)
  }
  object HttpError {
    def apply(
      statusCode: Int,
      statusLine: String = "",
      entity: Attempt[DataBuffer] = Attempt.success(DataBuffer.Empty)
    ): Attempt[HttpResponse] =
      entity.map(data => HttpResponse(statusCode, statusLine, data))
  }

  def notFound[A](jobId: JobId): Either[JobNotFound, A] =
    JobNotFound(jobId).asLeft[A]

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

class HttpProtocolSpec extends AsyncFlatSpec with HttpRequestMatchers with StubClient with EitherValues with Inside
    with ImplicitClock {
  import HttpProtocolSpec._

  implicit val scheduler: Scheduler = Scheduler.global
  override implicit def executionContext: ExecutionContext = scheduler

  // -- Subscribe

  "subscribe" should "return an stream of events" in {
    val givenNodeId = NodeId(UUID.randomUUID())
    val givenEvents = List(MasterJoined(givenNodeId, Location("localhost")), MasterReachable(givenNodeId))

    val httpEvents: Attempt[List[HttpServerSentEvent]] =
      EitherT(givenEvents.map(evt => DataBuffer(evt))).map(HttpServerSentEvent).value.sequenceU

    attempt2Future(httpEvents).flatMap { events =>
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
      not(matcher = hasPassport(TestPassport)) and
      hasTimeout(TestTimeout)

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
      recoverToSucceededIf[InvalidCredentials.type](client.authenticate("foo", "bar"))
    }
  }

  it should "result in an HTTP error if result code is not 401" in {
    inProtocol[HttpProtocol] ensuringRequest isLogin("foo", "bar") replyWith { _ =>
      HttpError(500, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[HttpError](client.authenticate("foo", "bar"))
    }
  }

  // -- Refresh token requests

  val isRefreshPassport = hasMethod(HttpMethod.Post) and
    hasUrl(uris.refreshPass) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

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
      recoverToSucceededIf[InvalidCredentials.type](client.refreshPassport)
    }
  }

  it should "result in an HTTP error if result code is not 401" in {
    inProtocol[HttpProtocol] ensuringRequest isRefreshPassport replyWith { _ =>
      HttpError(500, "TEST AUTH ERROR")
    } usingClient { client =>
      recoverToSucceededIf[HttpError](client.refreshPassport)
    }
  }

  // -- Logout requests

  val isLogout = hasMethod(HttpMethod.Post) and
    hasUrl(uris.logout) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

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
      recoverToSucceededIf[HttpError](client.signOut)
    }
  }

  // -- Get Cluster State

  val isGetClusterState = hasMethod(HttpMethod.Get) and
    hasUrl(uris.cluster) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

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
    hasBody(TestJobSpec) and
    isJsonRequest and
    hasTimeout(TestTimeout)

  "registerJob" should "return a validated JobId when it succeeds" in {
    inProtocol[HttpProtocol] ensuringRequest isRegisterJob replyWith { _ =>
      HttpSuccess(DataBuffer(TestJobId.validNel[QuckooError]))
    } usingClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId.toEither.right.value shouldBe TestJobId
      }
    }
  }

  it should "return the missed dependencies when fails to resolve" in {
    val missedDep                  = DownloadFailed(TestArtifactId, DownloadFailed.NotFound)
    val expectedError: QuckooError = MissingDependencies(NonEmptyList.of(missedDep))

    inProtocol[HttpProtocol] ensuringRequest isRegisterJob replyWith {
      _ => HttpSuccess(DataBuffer(expectedError.invalidNel[JobId]))
    } usingClient { client =>
      client.registerJob(TestJobSpec).map { validatedJobId =>
        validatedJobId shouldBe Validated.Invalid(NonEmptyList.of(expectedError))
      }
    }
  }

  // -- Fetch Jobs

  val isFetchJobs = hasMethod(HttpMethod.Get) and
    hasUrl(uris.jobs) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "fetchJobs" should "return a list of the job specs" in {
    inProtocol[HttpProtocol] ensuringRequest isFetchJobs replyWith { _ =>
      HttpSuccess(DataBuffer(List(TestJobId -> TestJobSpec)))
    } usingClient { client =>
      client.fetchJobs.map { jobs =>
        val jobMap = jobs.toMap
        jobMap should contain key TestJobId
        jobMap should contain value TestJobSpec
      }
    }
  }

  // -- Fetch Job

  val isFetchJob = hasMethod(HttpMethod.Get) and
    hasUrl(uris.fetchJob) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "fetchJob" should "return the job spec for a job ID" in {
    val urlPattern = uris.fetchJob.r
    inProtocol[HttpProtocol] ensuringRequest isFetchJob replyWith { req =>
      val urlPattern(id) = req.url
      if (JobId(id) == TestJobId) {
        HttpSuccess(DataBuffer(TestJobSpec))
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
      if (JobId(id) == TestJobId) HttpError(404, "TEST 404")
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
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "enableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.enableJob.r

    inProtocol[HttpProtocol] ensuringRequest isEnableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(JobEnabled(JobId(id))))
    } usingClient { client =>
      client.enableJob(TestJobId).map { ack =>
        ack shouldBe JobEnabled(TestJobId).asRight[JobNotFound]
      }
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.enableJob.r

    inProtocol[HttpProtocol] ensuringRequest isEnableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(JobId(id)))
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
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "disableJob" should "return the acknowledgement that the job is been disabled" in {
    val urlPattern = uris.disableJob.r

    inProtocol[HttpProtocol] ensuringRequest isDisableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpSuccess(DataBuffer(JobDisabled(JobId(id))))
    } usingClient { client =>
      client.disableJob(TestJobId).map { ack =>
        ack shouldBe JobDisabled(TestJobId).asRight[JobNotFound]
      }
    }
  }

  it should "return JobNotFound if the status code of the result is 404" in {
    val urlPattern = uris.disableJob.r

    inProtocol[HttpProtocol] ensuringRequest isDisableJob replyWith { req =>
      val urlPattern(id) = req.url
      HttpError(404, entity = DataBuffer(JobId(id)))
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
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "executionPlans" should "return a map with current exection plans" in {
    inProtocol[HttpProtocol] ensuringRequest isGetExecutionPlans replyWith {
      _ => HttpSuccess(DataBuffer(List(TestPlanId -> TestExecutionPlan)))
    } usingClient { client =>
      client.executionPlans.map { returnedPlans =>
        val returnedPlansMap = returnedPlans.toMap
        returnedPlansMap should contain key TestPlanId
        returnedPlansMap should contain value TestExecutionPlan
      }
    }
  }

  // -- Get execution plan

  val isGetExecutionPlan = hasMethod(HttpMethod.Get) and
    hasUrl(uris.executionPlan) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "executionPlan" should "return an execution plan for a given ID" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isGetExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (PlanId(UUID.fromString(id)) == TestPlanId) {
        HttpSuccess(DataBuffer(TestExecutionPlan))
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
      if (PlanId(UUID.fromString(id)) == TestPlanId) {
        HttpError(404, s"Should have returned a None instance")
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
    isJsonRequest and
    hasTimeout(TestTimeout)

  "scheduleJob" should "return the started notification" in {
    val payload = ScheduleJob(TestJobId)
    inProtocol[HttpProtocol] ensuringRequest isScheduleJob(payload) replyWith { req =>
      val result = req.entity.as[ScheduleJob].
        flatMap(cmd => DataBuffer(ExecutionPlanStarted(cmd.jobId, TestPlanId, currentDateTime)))
      HttpSuccess(result)
    } usingClient { client =>
      client.scheduleJob(payload).map { returned =>
        returned shouldBe ExecutionPlanStarted(TestJobId, TestPlanId, currentDateTime).asRight[JobNotFound]
      }
    }
  }

  it should "return JobNotFound if the HTTP status code is 404" in {
    val payload = ScheduleJob(TestJobId)
    inProtocol[HttpProtocol] ensuringRequest isScheduleJob(payload) replyWith { req =>
      val result = req.entity.as[ScheduleJob].
        flatMap(cmd => DataBuffer(cmd.jobId))
      HttpError(404, entity = result)
    } usingClient { client =>
      client.scheduleJob(payload).map { returned =>
        returned shouldBe JobNotFound(TestJobId).asLeft[ExecutionPlanStarted]
      }
    }
  }

  // -- Cancel execution plan

  val isCancelExecutionPlan = hasMethod(HttpMethod.Delete) and
    hasUrl(uris.executionPlan) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "cancelExecutionPlan" should "return nothing if the plan has been cancelled" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isCancelExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (PlanId(UUID.fromString(id)) == TestPlanId) {
        HttpSuccess(DataBuffer(ExecutionPlanCancelled(TestJobId, TestPlanId, currentDateTime)))
      } else {
        HttpError(500, s"Invalid plan id $id")
      }
    } usingClient { client =>
      client.cancelPlan(TestPlanId).map { returned =>
        returned.isRight shouldBe true
      }
    }
  }

  it should "return not found if the HTTP status code is 404" in {
    val urlPattern = uris.executionPlan.r
    inProtocol[HttpProtocol] ensuringRequest isCancelExecutionPlan replyWith { req =>
      val urlPattern(id) = req.url
      if (PlanId(UUID.fromString(id)) == TestPlanId) {
        HttpError(404, entity = DataBuffer(TestPlanId))
      } else {
        HttpError(500, s"Invalid plan id $id")
      }
    } usingClient { client =>
      client.cancelPlan(TestPlanId).map { returned =>
        returned shouldBe ExecutionPlanNotFound(TestPlanId).asLeft[ExecutionPlanCancelled]
      }
    }
  }

  // -- Get executions

  val isGetExecutions = hasMethod(HttpMethod.Get) and
    hasUrl(uris.executions) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "executions" should "return a map with the executions" in {
    inProtocol[HttpProtocol] ensuringRequest isGetExecutions replyWith {
      _ => HttpSuccess(DataBuffer(List(TestTaskId -> TestTaskExecution)))
    } usingClient { client =>
      client.executions.map { returnedExecutions =>
        val returnedExecutionsMap = returnedExecutions.toMap
        returnedExecutionsMap should contain key TestTaskId
        returnedExecutionsMap should contain value TestTaskExecution
      }
    }
  }

  // -- Get execution

  val isGetExecution = hasMethod(HttpMethod.Get) and
    hasUrl(uris.execution) and
    hasPassport(TestPassport) and
    hasEmptyBody and
    hasTimeout(TestTimeout)

  "execution" should "return an execution for a given ID" in {
    val urlPattern = uris.execution.r
    inProtocol[HttpProtocol] ensuringRequest isGetExecution replyWith { req =>
      val urlPattern(id) = req.url
      if (TaskId(UUID.fromString(id)) == TestTaskId) {
        HttpSuccess(DataBuffer(TestTaskExecution))
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
      if (TaskId(UUID.fromString(id)) == TestTaskId) {
        HttpError(404, s"Should have returned a None instance")
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
