package io.kairos.console.server.http

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest

import io.kairos.console.server.RegistryFacade
import io.kairos.fault.Fault
import io.kairos.id.{ArtifactId, JobId}
import io.kairos.protocol.RegistryProtocol.{JobDisabled, JobEnabled}
import io.kairos.{JobSpec, Validated, serialization}

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scalaz._

/**
  * Created by domingueza on 21/03/16.
  */
object RegistryHttpRouterSpec {

  final val TestJobSpec = JobSpec("TestJob",
    Some("Description for TestJob"),
    ArtifactId("org.example", "bar", "1.0.0"),
    "org.example.JobClass"
  )
  final val TestInvalidJobSpec = JobSpec("", None, ArtifactId("", "", ""), "")

  final val TestJobMap = Map(
    JobId(TestJobSpec) -> TestJobSpec
  )

}

class RegistryHttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers
    with RegistryHttpRouter with RegistryFacade {

  import RegistryHttpRouterSpec._
  import StatusCodes._
  import serialization.json.jvm._

  val wrappingRoute = pathPrefix("api" / "registry") {
    registryApi
  }

  override def enableJob(jobId: JobId): Future[JobEnabled] =
    Future.successful(JobEnabled(jobId))

  override def disableJob(jobId: JobId): Future[JobDisabled] =
    Future.successful(JobDisabled(jobId))

  override def registerJob(jobSpec: JobSpec): Future[Validated[JobId]] =
    Future.successful(JobSpec.validate(jobSpec)).map(validSpec => validSpec.map(JobId(_)))

  override def registeredJobs: Future[Map[JobId, JobSpec]] =
    Future.successful(TestJobMap)

  override def fetchJob(jobId: JobId): Future[Option[JobSpec]] =
    Future.successful(TestJobMap.get(jobId))

  private[this] def endpoint(target: String) = s"/api/registry$target"

  "The Registry API" should {

    "return a map of jobs" in {
      Get(endpoint("/jobs")) ~> wrappingRoute ~> check {
        responseAs[Map[JobId, JobSpec]] should be (TestJobMap)
      }
    }

    "return a JobId if the job spec is valid" in {
      import Scalaz._

      Put(endpoint("/jobs"), Some(TestJobSpec)) ~> wrappingRoute ~> check {
        responseAs[Validated[JobId]] should be (JobId(TestJobSpec).successNel[Fault])
      }
    }

    "return validation errors if the job spec is invalid" in {
      Put(endpoint("/jobs"), Some(TestInvalidJobSpec)) ~> wrappingRoute ~> check {
        responseAs[Validated[JobId]] should be (JobSpec.validate(TestInvalidJobSpec))
      }
    }

    "return 404 if the job id does not exist" in {
      val randomId = UUID.randomUUID()
      Get(endpoint(s"/jobs/$randomId")) ~> wrappingRoute ~> check {
        status === NotFound
      }
    }

    "return a job spec if the ID exists" in {
      Get(endpoint(s"/jobs/${JobId(TestJobSpec)}")) ~> wrappingRoute ~> check {
        responseAs[JobSpec] should be (TestJobSpec)
      }
    }

    "return a job enabled message if enabling succeeds" in {
      val jobId = JobId(TestJobSpec)
      Post(endpoint(s"/jobs/$jobId/enable")) ~> wrappingRoute ~> check {
        responseAs[JobEnabled] should be (JobEnabled(jobId))
      }
    }

    "return a job disabled message if enabling succeeds" in {
      val jobId = JobId(TestJobSpec)
      Post(endpoint(s"/jobs/$jobId/disable")) ~> wrappingRoute ~> check {
        responseAs[JobDisabled] should be (JobDisabled(jobId))
      }
    }

  }

}
