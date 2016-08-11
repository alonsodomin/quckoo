package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.NotUsed
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source

import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.id.{ArtifactId, JobId, PlanId, TaskId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo._

import org.scalatest.{Matchers, WordSpec}
import org.threeten.bp.{Clock, Instant, ZoneId, ZonedDateTime}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by domingueza on 21/03/16.
  */
object SchedulerHttpRouterSpec {

  final val FixedInstant = Instant.ofEpochMilli(8939283923L)
  final val FixedClock = Clock.fixed(FixedInstant, ZoneId.of("UTC"))

  final val TestJobId = JobId(UUID.randomUUID())
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
  final val TestTask = Task(TestTaskIds.head, ArtifactId("com.example", "example", "latest"), "")
  final val TestTaskMap = Map(
    TestTaskIds.head -> TaskExecution(
      TestPlanIds.head, TestTask, TaskExecution.Scheduled, None
    )
  )

}

class SchedulerHttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers
    with SchedulerHttpRouter with SchedulerApi with SchedulerStreams {

  import SchedulerHttpRouterSpec._
  import StatusCodes._
  import serialization.json._

  val entryPoint = pathPrefix("api" / "scheduler") {
    schedulerApi
  }

  override def cancelPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Unit] =
    Future.successful(())

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] =
    Future.successful(TestPlanMap.get(planId))

  override def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    val response = TestPlanMap.values.find(_.jobId == schedule.jobId) match {
      case Some(plan) => Right(ExecutionPlanStarted(schedule.jobId, plan.planId))
      case _          => Left(JobNotFound(schedule.jobId))
    }
    Future.successful(response)
  }

  override def executionPlans(implicit ec: ExecutionContext): Future[Map[PlanId, ExecutionPlan]] =
    Future.successful(TestPlanMap)

  override def executions(implicit ec: ExecutionContext): Future[Map[TaskId, TaskExecution]] =
    Future.successful(TestTaskMap)

  override def execution(taskId: TaskId)(implicit ec: ExecutionContext): Future[Option[TaskExecution]] =
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
      val scheduleMsg = ScheduleJob(JobId(UUID.randomUUID()))
      Post(endpoint(s"/plans"), Some(scheduleMsg)) ~> entryPoint ~> check {
        status === NotFound
        responseAs[JobId] should be (scheduleMsg.jobId)
      }
    }

    "return the execution plan id when scheduling a job" in {
      val scheduleMsg = ScheduleJob(TestPlanMap.head._2.jobId)
      Post(endpoint(s"/plans"), Some(scheduleMsg)) ~> entryPoint ~> check {
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
