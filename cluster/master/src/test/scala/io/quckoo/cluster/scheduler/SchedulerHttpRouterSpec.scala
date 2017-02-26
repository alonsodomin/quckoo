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

package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.NotUsed
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source

import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.id.{ArtifactId, JobId, PlanId, TaskId}
import io.quckoo.protocol.scheduler._
import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.fault._
import io.quckoo.serialization.DataBuffer
import io.quckoo.testkit.ImplicitClock

import org.scalatest.{Matchers, WordSpec}
import org.threeten.bp.{Clock, Instant, ZoneId, ZonedDateTime}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import scalaz.syntax.either._

/**
  * Created by domingueza on 21/03/16.
  */
object SchedulerHttpRouterSpec {

  final val FixedInstant = Instant.ofEpochMilli(8939283923L)
  final val FixedClock = Clock.fixed(FixedInstant, ZoneId.of("UTC"))

  implicit final val TestTimeout = 1 second
  implicit final val TestPassport = {
    val header = DataBuffer.fromString("{}").toBase64
    val claims = DataBuffer.fromString("{}").toBase64
    val signature = DataBuffer.fromString(System.currentTimeMillis().toString).toBase64
    new Passport(Map.empty, s"$header.$claims.$signature")
  }

  final val TestJobId = JobId("jobId")
  final val TestPlanIds = Set(UUID.randomUUID())
  final val TestPlanMap = Map(
    TestPlanIds.head -> ExecutionPlan(
      TestJobId,
      TestPlanIds.head,
      Trigger.Immediate,
      ZonedDateTime.now(FixedClock)
    )
  )

  final val TestTaskIds: Seq[TaskId] = List(UUID.randomUUID())
  final val TestTask = Task(TestTaskIds.head, JobPackage.jar(
    ArtifactId("com.example", "example", "latest"), ""))
  final val TestTaskMap = Map(
    TestTaskIds.head -> TaskExecution(
      TestPlanIds.head, TestTask, TaskExecution.Scheduled, None
    )
  )

}

class SchedulerHttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers
    with SchedulerHttpRouter with SchedulerApi with SchedulerStreams with ImplicitClock {

  import SchedulerHttpRouterSpec._
  import StatusCodes._
    import serialization.json._

  val entryPoint = pathPrefix("api" / "scheduler") {
    schedulerApi
  }

  override def cancelPlan(planId: PlanId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[ExecutionPlanNotFound \/ ExecutionPlanCancelled] = Future.successful {
    TestPlanMap.get(planId).
      map(plan => ExecutionPlanCancelled(plan.jobId, planId, ZonedDateTime.now(clock)).right[ExecutionPlanNotFound]).
      getOrElse(ExecutionPlanNotFound(planId).left[ExecutionPlanCancelled])
  }

  override def executionPlan(planId: PlanId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Option[ExecutionPlan]] =
    Future.successful(TestPlanMap.get(planId))

  override def scheduleJob(schedule: ScheduleJob)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[JobNotFound \/ ExecutionPlanStarted] = Future.successful {
    TestPlanMap.values.find(_.jobId == schedule.jobId).
      map(plan => ExecutionPlanStarted(schedule.jobId, plan.planId, ZonedDateTime.now(clock)).right[JobNotFound]).
      getOrElse(JobNotFound(schedule.jobId).left[ExecutionPlanStarted])
  }

  override def executionPlans(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Map[PlanId, ExecutionPlan]] =
    Future.successful(TestPlanMap)

  override def executions(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Map[TaskId, TaskExecution]] =
    Future.successful(TestTaskMap)

  override def execution(taskId: TaskId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Option[TaskExecution]] =
    Future.successful(TestTaskMap.get(taskId))

  override def schedulerEvents: Source[SchedulerEvent, NotUsed] = ???

  private[this] def endpoint(target: String) = s"/api/scheduler$target"

  "The Scheduler API" should {

    "reply with a map of execution plans" in {
      Get(endpoint("/plans")) ~> entryPoint ~> check {
        responseAs[Map[PlanId, ExecutionPlan]] should be (TestPlanMap)
      }
    }

    "return 404 when asked for a plan that does not exist" in {
      Get(endpoint(s"/plans/${UUID.randomUUID()}")) ~> entryPoint ~> check {
        status === NotFound
      }
    }

    "return an execution plan when getting from a valid ID" in {
      Get(endpoint(s"/plans/${TestPlanIds.head}")) ~> entryPoint ~> check {
        responseAs[ExecutionPlan].planId should be (TestPlanIds.head)
      }
    }

    "cancel an execution plan on a DELETE http method" in {
      Delete(endpoint(s"/plans/${UUID.randomUUID()}")) ~> entryPoint ~> check {
        status === OK
      }
    }

    "return a 404 when scheduling a job that does not exist" in {
      val scheduleMsg = ScheduleJob(JobId("fooJobId"))
      Put(endpoint(s"/plans"), Some(scheduleMsg)) ~> entryPoint ~> check {
        status === NotFound
        responseAs[JobId] should be (scheduleMsg.jobId)
      }
    }

    "return ExecutionPlanStarted when scheduling a job" in {
      val scheduleMsg = ScheduleJob(TestPlanMap.head._2.jobId)
      Put(endpoint(s"/plans"), Some(scheduleMsg)) ~> entryPoint ~> check {
        status === OK

        val response = responseAs[ExecutionPlanStarted]
        response.jobId should be (scheduleMsg.jobId)
      }
    }

    "reply with a list of task ids" in {
      Get(endpoint("/executions")) ~> entryPoint ~> check {
        responseAs[Map[TaskId, TaskExecution]] shouldBe TestTaskMap
      }
    }

    "return a task when getting from a valid ID" in {
      val taskId = TestTaskIds.head
      Get(endpoint(s"/executions/$taskId")) ~> entryPoint ~> check {
        responseAs[TaskExecution] shouldBe TestTaskMap(taskId)
      }
    }

    "return a 404 when asked for a task that does not exist" in {
      val taskId = UUID.randomUUID()
      Get(endpoint(s"/executions/$taskId")) ~> entryPoint ~> check {
        status === NotFound
      }
    }

  }

}
