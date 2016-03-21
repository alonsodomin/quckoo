package io.kairos.console.server.http

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.kairos.{ExecutionPlan, Trigger, serialization}
import io.kairos.console.server.core.SchedulerFacade
import io.kairos.id.{JobId, PlanId}
import io.kairos.protocol.SchedulerProtocol
import io.kairos.time.JDK8TimeSource
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

/**
  * Created by domingueza on 21/03/16.
  */
object SchedulerHttpRouterSpec {

  final val TestPlanIds = List(UUID.randomUUID())

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
    with SchedulerHttpRouter with SchedulerFacade {

  import SchedulerHttpRouterSpec._
  import SchedulerProtocol._
  import StatusCodes._
  import serialization.json.jvm._

  val entryPoint = pathPrefix("api" / "scheduler") {
    schedulerApi
  }

  override def executionPlan(planId: PlanId): Future[Option[ExecutionPlan]] =
    Future.successful(TestPlanMap.get(planId))

  override def schedule(schedule: ScheduleJob): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    val response = TestPlanMap.values.find(_.jobId == schedule.jobId) match {
      case Some(plan) => Right(ExecutionPlanStarted(schedule.jobId, plan.planId))
      case _          => Left(JobNotFound(schedule.jobId))
    }
    Future.successful(response)
  }

  override def allExecutionPlanIds: Future[List[PlanId]] =
    Future.successful(TestPlanIds)

  private[this] def endpoint(target: String) = s"/api/scheduler$target"

  "The Scheduler API" should {

    "reply a list of plan ids" in {
      Get(endpoint("/plans")) ~> entryPoint ~> check {
        responseAs[List[PlanId]] should be (TestPlanIds)
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
