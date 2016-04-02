package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.NotUsed
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source

import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization
import io.quckoo.time.JDK8TimeSource
import io.quckoo.{ExecutionPlan, Trigger}

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by domingueza on 21/03/16.
  */
object SchedulerHttpRouterSpec {

  final val TestPlanIds = Set(UUID.randomUUID())

  final val TestPlanMap = Map(
    TestPlanIds.head -> ExecutionPlan(
      JobId(UUID.randomUUID()),
      TestPlanIds.head,
      Trigger.Immediate,
      JDK8TimeSource.default.currentDateTime
    )
  )

}

class SchedulerHttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers
    with SchedulerHttpRouter with SchedulerApi with SchedulerStreams {

  import SchedulerHttpRouterSpec._
  import StatusCodes._
  import serialization.json.jvm._

  val entryPoint = pathPrefix("api" / "scheduler") {
    schedulerApi
  }

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] =
    Future.successful(TestPlanMap.get(planId))

  override def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    val response = TestPlanMap.values.find(_.jobId == schedule.jobId) match {
      case Some(plan) => Right(ExecutionPlanStarted(schedule.jobId, plan.planId))
      case _          => Left(JobNotFound(schedule.jobId))
    }
    Future.successful(response)
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]] =
    Future.successful(TestPlanIds)

  override def workerEvents: Source[WorkerEvent, NotUsed] = ???

  private[this] def endpoint(target: String) = s"/api/scheduler$target"

  "The Scheduler API" should {

    "reply a list of plan ids" in {
      Get(endpoint("/plans")) ~> entryPoint ~> check {
        responseAs[Set[PlanId]] should be (TestPlanIds)
      }
    }

    "return 404 when asked for a plan that does not exist" in {
      Get(endpoint(s"/plans/${UUID.randomUUID()}")) ~> entryPoint ~> check {
        status === NotFound
      }
    }

    "return an execution plan when getting from a valid ID" in {
      Get(endpoint(s"/plans/${TestPlanIds.head}")) ~> entryPoint ~> check {
        responseAs[ExecutionPlan] should be (TestPlanMap(TestPlanIds.head))
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

  }

}
