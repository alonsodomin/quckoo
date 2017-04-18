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

import cats.data.ValidatedNel
import cats.syntax.either._
import cats.syntax.validated._

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

import io.circe.generic.auto._

import io.quckoo._
import io.quckoo.api.{Registry => RegistryApi}
import io.quckoo.auth.Passport
import io.quckoo.protocol.registry._
import io.quckoo.serialization.DataBuffer

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by domingueza on 21/03/16.
  */
object RegistryHttpRouterSpec {

  final val TestJobSpec = JobSpec("TestJob",
    Some("Description for TestJob"),
    JobPackage.jar(
      ArtifactId("org.example", "bar", "1.0.0"),
      "org.example.JobClass"
    )
  )
  final val TestInvalidJobSpec = JobSpec("", None, JobPackage.jar(ArtifactId("", "", ""), ""))

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
  import ErrorAccumulatingCirceSupport._

  val entryPoint = pathPrefix("api" / "registry") {
    registryApi
  }

  override def enableJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Either[JobNotFound, JobEnabled]] = {
    val response = {
      if (TestJobMap.contains(jobId)) JobEnabled(jobId).asRight[JobNotFound]
      else JobNotFound(jobId).asLeft[JobEnabled]
    }
    Future.successful(response)
  }

  override def disableJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Either[JobNotFound, JobDisabled]] = {
    val response = {
      if (TestJobMap.contains(jobId)) JobDisabled(jobId).asRight[JobNotFound]
      else JobNotFound(jobId).asLeft[JobDisabled]
    }
    Future.successful(response)
  }

  override def registerJob(jobSpec: JobSpec)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[ValidatedNel[Fault, JobId]] = Future.successful {
    JobSpec.valid.run(jobSpec)
      .map(JobId(_))
      .leftMap(vs => ValidationFault(vs).asInstanceOf[Fault])
      .toValidatedNel
  }

  override def fetchJobs(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Seq[(JobId, JobSpec)]] =
    Future.successful(TestJobMap.toSeq)

  override def fetchJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: FiniteDuration, passport: Passport
  ): Future[Option[JobSpec]] =
    Future.successful(TestJobMap.get(jobId))

  override def registryTopic: Source[RegistryEvent, NotUsed] = ???

  private[this] def endpoint(target: String) = s"/api/registry$target"

  "The Registry API" should {

    "return a map of jobs" in {
      Get(endpoint("/jobs")) ~> entryPoint ~> check {
        responseAs[Map[JobId, JobSpec]] shouldBe TestJobMap
      }
    }

    "return a JobId if the job spec is valid" in {
      Put(endpoint("/jobs"), Some(TestJobSpec)) ~> entryPoint ~> check {
        responseAs[ValidatedNel[Fault, JobId]] shouldBe JobId(TestJobSpec).validNel[Fault]
      }
    }

    "return validation errors if the job spec is invalid" in {
      val expectedResponse = JobSpec.valid.run(TestInvalidJobSpec)
        .map(JobId(_))
        .leftMap(vs => ValidationFault(vs).asInstanceOf[Fault])
        .toValidatedNel

      Put(endpoint("/jobs"), Some(TestInvalidJobSpec)) ~> entryPoint ~> check {
        status === BadRequest
        responseAs[ValidatedNel[Fault, JobId]] shouldBe expectedResponse
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
      val id = "fooId"
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
      val id = "barId"
      Post(endpoint(s"/jobs/$id/disable")) ~> entryPoint ~> check {
        responseAs[JobId] shouldBe JobId(id)
        status === NotFound
      }
    }

  }

}
