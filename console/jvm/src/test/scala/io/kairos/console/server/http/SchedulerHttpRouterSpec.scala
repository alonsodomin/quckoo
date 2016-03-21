package io.kairos.console.server.http

import java.util.UUID

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest

import io.kairos.ExecutionPlan
import io.kairos.console.server.core.SchedulerFacade
import io.kairos.id.PlanId
import io.kairos.protocol.SchedulerProtocol.{ExecutionPlanStarted, JobNotFound, ScheduleJob}

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

/**
  * Created by domingueza on 21/03/16.
  */
object SchedulerHttpRouterSpec {

  final val TestPlanIds = List(UUID.randomUUID(), UUID.randomUUID())

}

class SchedulerHttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers
    with SchedulerHttpRouter with SchedulerFacade {

  import SchedulerHttpRouterSpec._

  val entryPoint = pathPrefix("api" / "scheduler") {
    schedulerApi
  }

  override def executionPlan(planId: PlanId): Future[ExecutionPlan] = ???

  override def schedule(schedule: ScheduleJob): Future[Either[JobNotFound, ExecutionPlanStarted]] = ???

  override def allExecutionPlanIds: Future[List[PlanId]] =
    Future.successful(TestPlanIds)

  private[this] def endpoint(target: String) = s"/api/scheduler$target"

  "The Scheduler API" should {

    "reply a list of plan ids" in {
      Get(endpoint("/plans")) ~> entryPoint ~> check {
        responseAs[List[PlanId]] should be (TestPlanIds)
      }
    }

  }

}
