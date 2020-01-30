/*
 * Copyright 2015 A. Alonso Dominguez
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
import java.time.{Clock, Instant, ZoneId, ZonedDateTime}

import akka.NotUsed
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source

import cats.syntax.either._

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

import io.circe.generic.auto._
import io.circe.java8.time._

import io.quckoo._
import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.protocol.scheduler._
import io.quckoo.auth.Passport
import io.quckoo.serialization.DataBuffer
import io.quckoo.testkit.ImplicitClock

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by domingueza on 21/03/16.
  */
object SchedulerHttpRouterSpec {

  final val FixedInstant = Instant.ofEpochMilli(8939283923L)
  final val FixedClock   = Clock.fixed(FixedInstant, ZoneId.of("UTC"))

  implicit final val TestTimeout = 1 second
  implicit final val TestPassport = {
    val header = DataBuffer.fromString("{}").toBase64
    val claims = DataBuffer.fromString("{}").toBase64
    val signature =
      DataBuffer.fromString(System.currentTimeMillis().toString).toBase64
    new Passport(Map.empty, s"$header.$claims.$signature")
  }

  final val TestJobId   = JobId("jobId")
  final val TestPlanIds = Set(PlanId(UUID.randomUUID()))
  final val TestPlanMap = Map(
    TestPlanIds.head -> ExecutionPlan(
      TestJobId,
      TestPlanIds.head,
      Trigger.Immediate,
      ZonedDateTime.now(FixedClock)
    )
  )

  final val TestTaskIds: Seq[TaskId] = List(TaskId(UUID.randomUUID()))
  final val TestTask =
    Task(TestTaskIds.head, JobPackage.jar(ArtifactId("com.example", "example", "latest"), ""))
  final val TestTaskMap = Map(
    TestTaskIds.head -> TaskExecution(
      TestPlanIds.head,
      TestTask,
      TaskExecution.Status.Scheduled,
      None
    )
  )

}

class SchedulerHttpRouterSpec
    extends WordSpec with ScalatestRouteTest with Matchers with SchedulerHttpRouter
    with SchedulerApi with SchedulerStreams with ImplicitClock {

  import SchedulerHttpRouterSpec._
  import StatusCodes._
  import serialization.json._
  import ErrorAccumulatingCirceSupport._

  val entryPoint = pathPrefix("api" / "scheduler") {
    schedulerApi
  }

  override def cancelPlan(planId: PlanId)(
      implicit
      ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] =
    Future.successful {
      TestPlanMap
        .get(planId)
        .map(
          plan =>
            ExecutionPlanCancelled(plan.jobId, planId, ZonedDateTime.now(clock))
              .asRight[ExecutionPlanNotFound]
        )
        .getOrElse(ExecutionPlanNotFound(planId).asLeft[ExecutionPlanCancelled])
    }

  override def executionPlan(planId: PlanId)(
      implicit
      ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[ExecutionPlan]] =
    Future.successful(TestPlanMap.get(planId))

  override def scheduleJob(schedule: ScheduleJob)(
      implicit
      ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, ExecutionPlanStarted]] = Future.successful {
    TestPlanMap.values
      .find(_.jobId == schedule.jobId)
      .map(
        plan =>
          ExecutionPlanStarted(schedule.jobId, plan.planId, ZonedDateTime.now(clock))
            .asRight[JobNotFound]
      )
      .getOrElse(JobNotFound(schedule.jobId).asLeft[ExecutionPlanStarted])
  }

  override def executionPlans(
      implicit
      ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(PlanId, ExecutionPlan)]] =
    Future.successful(TestPlanMap.toSeq)

  override def executions(
      implicit
      ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(TaskId, TaskExecution)]] =
    Future.successful(TestTaskMap.toSeq)

  override def execution(taskId: TaskId)(
      implicit
      ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[TaskExecution]] =
    Future.successful(TestTaskMap.get(taskId))

  override def schedulerTopic: Source[SchedulerEvent, NotUsed] = ???

  private[this] def endpoint(target: String) = s"/api/scheduler$target"

  "The Scheduler API" should {

    "reply with a sequence of execution plans with their ids" in {
      Get(endpoint("/plans")) ~> entryPoint ~> check {
        responseAs[Seq[(PlanId, ExecutionPlan)]] shouldBe TestPlanMap.toSeq
      }
    }

    "return 404 when asked for a plan that does not exist" in {
      Get(endpoint(s"/plans/${UUID.randomUUID()}")) ~> entryPoint ~> check {
        status === NotFound
      }
    }

    "return an execution plan when getting from a valid ID" in {
      Get(endpoint(s"/plans/${TestPlanIds.head}")) ~> entryPoint ~> check {
        responseAs[ExecutionPlan].planId should be(TestPlanIds.head)
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
        responseAs[JobId] should be(scheduleMsg.jobId)
      }
    }

    "return ExecutionPlanStarted when scheduling a job" in {
      val scheduleMsg = ScheduleJob(TestPlanMap.head._2.jobId)
      Put(endpoint(s"/plans"), Some(scheduleMsg)) ~> entryPoint ~> check {
        status === OK

        val response = responseAs[ExecutionPlanStarted]
        response.jobId should be(scheduleMsg.jobId)
      }
    }

    "reply with a sequence of tasks and their ids" in {
      Get(endpoint("/executions")) ~> entryPoint ~> check {
        responseAs[Seq[(TaskId, TaskExecution)]] shouldBe TestTaskMap.toSeq
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
