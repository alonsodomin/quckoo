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

package io.quckoo.cluster.registry

import java.util.UUID

import akka.NotUsed
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source

import io.quckoo.fault._
import io.quckoo.api.{Registry => RegistryApi}
import io.quckoo.auth.Passport
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.serialization.DataBuffer
import io.quckoo.{JobSpec, serialization}

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import scalaz.syntax.either._
import scalaz.syntax.validation._

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

  implicit final val TestTimeout = 1 second
  implicit final val TestPassport = {
    val header = DataBuffer.fromString("{}").toBase64
    val claims = DataBuffer.fromString("{}").toBase64
    val signature = DataBuffer.fromString(System.currentTimeMillis().toString).toBase64
    new Passport(Map.empty, s"$header.$claims.$signature")
  }

}

class RegistryHttpRouterSpec extends WordSpec with ScalatestRouteTest with Matchers
    with RegistryHttpRouter with RegistryApi with RegistryStreams {

  import RegistryHttpRouterSpec._
  import StatusCodes._
  import serialization.json._

  val entryPoint = pathPrefix("api" / "registry") {
    registryApi
  }

  override def enableJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[JobNotFound \/ JobEnabled] = {
    val response = {
      if (TestJobMap.contains(jobId)) JobEnabled(jobId).right[JobNotFound]
      else JobNotFound(jobId).left[JobEnabled]
    }
    Future.successful(response)
  }

  override def disableJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[JobNotFound \/ JobDisabled] = {
    val response = {
      if (TestJobMap.contains(jobId)) JobDisabled(jobId).right[JobNotFound]
      else JobNotFound(jobId).left[JobDisabled]
    }
    Future.successful(response)
  }

  override def registerJob(jobSpec: JobSpec)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[ValidationNel[Fault, JobId]] = Future.successful {
    EitherT.fromDisjunction(JobSpec.validate(jobSpec).disjunction).
      map(JobId(_)).leftMap(_.map(_.asInstanceOf[Fault])).
      run.validation
  }

  override def fetchJobs(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Map[JobId, JobSpec]] =
    Future.successful(TestJobMap)

  override def fetchJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Option[JobSpec]] =
    Future.successful(TestJobMap.get(jobId))

  override def registryEvents: Source[RegistryEvent, NotUsed] = ???

  private[this] def endpoint(target: String) = s"/api/registry$target"

  "The Registry API" should {

    "return a map of jobs" in {
      Get(endpoint("/jobs")) ~> entryPoint ~> check {
        responseAs[Map[JobId, JobSpec]] shouldBe TestJobMap
      }
    }

    "return a JobId if the job spec is valid" in {
      Put(endpoint("/jobs"), Some(TestJobSpec)) ~> entryPoint ~> check {
        responseAs[JobId] shouldBe JobId(TestJobSpec)
      }
    }

    "return validation errors if the job spec is invalid" in {
      Put(endpoint("/jobs"), Some(TestInvalidJobSpec)) ~> entryPoint ~> check {
        status == BadRequest
        responseAs[NonEmptyList[Fault]].failure[JobId] shouldBe JobSpec.validate(TestInvalidJobSpec)
      }
    }

    "return 404 if the job id does not exist" in {
      val randomId = UUID.randomUUID()
      Get(endpoint(s"/jobs/$randomId")) ~> entryPoint ~> check {
        status === NotFound
      }
    }

    "return a job spec if the ID exists" in {
      Get(endpoint(s"/jobs/${JobId(TestJobSpec)}")) ~> entryPoint ~> check {
        responseAs[JobSpec] shouldBe TestJobSpec
      }
    }

    "return a job enabled message if enabling succeeds" in {
      val jobId = JobId(TestJobSpec)
      Post(endpoint(s"/jobs/$jobId/enable")) ~> entryPoint ~> check {
        responseAs[JobEnabled] shouldBe JobEnabled(jobId)
      }
    }

    "return 404 when enabling a job if it does not exist" in {
      val id = UUID.randomUUID()
      Post(endpoint(s"/jobs/$id/enable")) ~> entryPoint ~> check {
        responseAs[JobId] shouldBe JobId(id)
        status === NotFound
      }
    }

    "return a job disabled message if enabling succeeds" in {
      val jobId = JobId(TestJobSpec)
      Post(endpoint(s"/jobs/$jobId/disable")) ~> entryPoint ~> check {
        responseAs[JobDisabled] shouldBe JobDisabled(jobId)
      }
    }

    "return 404 when disabling a job if it does not exist" in {
      val id = UUID.randomUUID()
      Post(endpoint(s"/jobs/$id/disable")) ~> entryPoint ~> check {
        responseAs[JobId] shouldBe JobId(id)
        status === NotFound
      }
    }

  }

}
