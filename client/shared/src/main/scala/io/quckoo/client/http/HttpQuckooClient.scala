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

package io.quckoo.client.http

import java.nio.ByteBuffer

import cats.data._
import cats.effect._
import cats.implicits._

import io.quckoo._
import io.quckoo.client._
import io.quckoo.auth.{InvalidCredentials, Passport}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.json._

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._

import io.circe.{Error => JsonError, _}

import monix.eval.Task
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Future

abstract class HttpQuckooClient private[http] (baseUri: Option[Uri])(
    implicit backend: SttpBackend[Task, Observable[ByteBuffer]]
) extends QuckooClient {

  private[this] def buildUri(path: String): Uri =
    baseUri.map(_.path(path)).getOrElse(uri"$path")

  def signIn(username: String, password: String): ClientIO[Unit] = {
    def decodeLoginBody(body: Either[String, String]): Either[Throwable, Passport] = {
      val invalid: Either[Throwable, String] = body.leftMap(_ => InvalidCredentials)
      invalid >>= Passport.apply
    }

    for {
      request <- ClientIO.pure(
        sttp.post(buildUri(LoginURI)).auth.basic(username, password)
      )
      response <- ClientIO.fromEffect(request.send())
      passport <- ClientIO.fromAttempt(decodeLoginBody(response.body))
      _        <- ClientIO.setPassport(passport)
    } yield ()
  }

  def signOut(): ClientIO[Unit] =
    for {
      request  <- ClientIO.auth.map(_.post(buildUri(LogoutURI)))
      response <- ClientIO.fromEffect(request.send())
      _        <- ClientIO.fromEither(response.body)
      _        <- ClientIO.clearPassport()
    } yield ()

  def clusterState: ClientIO[QuckooState] = for {
    request  <- ClientIO.auth.map(_.get(buildUri(ClusterStateURI)).response(asJson[QuckooState]))
    response <- ClientIO.fromEffect(request.send())
    body     <- ClientIO.fromEither(response.body)
    result   <- ClientIO.fromAttempt(body)
  } yield result

  // -- Registry

  def registerJob(jobSpec: JobSpec): ClientIO[ValidatedNel[QuckooError, JobId]] = {
    // The Scala compiler needs some help resolving this decoder
    implicit lazy val resultDec: Decoder[ValidatedNel[QuckooError, JobId]] =
      Decoder[ValidatedNel[QuckooError, JobId]]

    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI").body(jobSpec).response(asJson[ValidatedNel[QuckooError, JobId]])
      )
      response <- ClientIO.fromEffect(request.send())
      body     <- ClientIO.fromEither(response.body)
      result  <- ClientIO.fromAttempt(body)
    } yield result
  }

  def fetchJob(jobId: JobId): ClientIO[Option[JobSpec]] =
    for {
      request <- ClientIO.auth.map(_.get(uri"$JobsURI/$jobId").response(asJson[JobSpec]))
      response <- ClientIO.fromEffect(request.send())
      body  <- ClientIO.optionalBody(response)
      result <- ClientIO.fromAttempt(body.sequence)
    } yield result

  def fetchJobs(): ClientIO[List[(JobId, JobSpec)]] =
    for {
      request <- ClientIO.auth.map(_.get(uri"$JobsURI").response(asJson[List[(JobId, JobSpec)]]))
      response <- ClientIO.fromEffect(request.send())
      body     <- ClientIO.fromEither(response.body)
      result  <- ClientIO.fromAttempt(body)
    } yield result

  def enableJob(jobId: JobId): ClientIO[Unit] =
    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI/$jobId/enable").response(asJson[JobEnabled])
      )
      response <- ClientIO.fromEffect(request.send())
      _        <- if (response.code == 404) ClientIO.raiseError(JobNotFound(jobId)) else ClientIO.unit
    } yield ()

  def disableJob(jobId: JobId): ClientIO[Unit] =
    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI/$jobId/disable").response(asJson[JobDisabled])
      )
      response <- ClientIO.fromEffect(request.send())
      _        <- if (response.code == 404) ClientIO.raiseError(JobNotFound(jobId)) else ClientIO.unit
    } yield ()

  // -- Scheduler

  def startPlan(schedule: ScheduleJob): ClientIO[ExecutionPlanStarted] =
    for {
      request <- ClientIO.auth.map(
        _.put(uri"$ExecutionPlansURI").body(schedule).response(asJson[ExecutionPlanStarted])
      )
      response <- ClientIO.fromEffect(request.send())
      body <- if (response.code == 404) ClientIO.raiseError(JobNotFound(schedule.jobId))
      else ClientIO.fromEither(response.body)
      result <- ClientIO.fromAttempt(body)
    } yield result

  def cancelPlan(planId: PlanId): ClientIO[ExecutionPlanCancelled] =
    for {
      request  <- ClientIO.auth.map(_.delete(uri"$ExecutionPlansURI/$planId").response(asJson[ExecutionPlanCancelled]))
      response <- ClientIO.fromEffect(request.send())
      body <- if (response.code == 404) ClientIO.raiseError(ExecutionPlanNotFound(planId))
      else ClientIO.fromEither(response.body)
      result <- ClientIO.fromAttempt(body)
    } yield result

  def fetchPlans(): ClientIO[List[(PlanId, ExecutionPlan)]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$ExecutionPlansURI").response(asJson[List[(PlanId, ExecutionPlan)]])
      )
      response <- ClientIO.fromEffect(request.send())
      body     <- ClientIO.fromEither(response.body)
      result  <- ClientIO.fromAttempt(body)
    } yield result

  def fetchPlan(planId: PlanId): ClientIO[Option[ExecutionPlan]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$ExecutionPlansURI/$planId").response(asJson[ExecutionPlan])
      )
      response <- ClientIO.fromEffect(request.send())
      body  <- ClientIO.optionalBody(response)
      result <- ClientIO.fromAttempt(body.sequence)
    } yield result

  def fetchTasks(): ClientIO[List[(TaskId, TaskExecution)]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$TaskExecutionsURI").response(asJson[List[(TaskId, TaskExecution)]])
      )
      response <- ClientIO.fromEffect(request.send())
      body     <- ClientIO.fromEither(response.body)
      result  <- ClientIO.fromAttempt(body)
    } yield result

  def fetchTask(taskId: TaskId): ClientIO[Option[TaskExecution]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$TaskExecutionsURI/$taskId").response(asJson[TaskExecution])
      )
      response <- ClientIO.fromEffect(request.send())
      body  <- ClientIO.optionalBody(response)
      result <- ClientIO.fromAttempt(body.sequence)
    } yield result

}
