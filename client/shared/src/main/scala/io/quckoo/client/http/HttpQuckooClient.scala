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

import cats.data._
import cats.effect._
import cats.implicits._

import io.quckoo._
import io.quckoo.client._
import io.quckoo.auth.{InvalidCredentials, Passport}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.json._

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._

import io.circe.{Error => JsonError, _}

import scala.concurrent.Future

class HttpQuckooClient private[http] (host: String, port: Int)(
    implicit backend: SttpBackend[Future, _]
) extends QuckooClient {
  import QuckooClient._

  def signIn(username: String, password: String): ClientIO[Unit] = {
    def decodeLoginBody(body: Either[String, String]): Either[Throwable, Passport] = {
      val invalid: Either[Throwable, String] = body.leftMap(_ => InvalidCredentials)
      invalid >>= Passport.apply
    }

    for {
      request <- ClientIO.pure(
        sttp.post(uri"$LoginURI").auth.basic(username, password)
      )
      response <- ClientIO.fromFuture(request.send())
      passport <- ClientIO.fromAttempt(decodeLoginBody(response.body))
      _        <- ClientIO.setPassport(passport)
    } yield ()
  }

  def signOut(): ClientIO[Unit] =
    for {
      request <- ClientIO.auth.map(_.post(uri"$LogoutURI"))
      _       <- ClientIO.handle(request)
    } yield ()

  // -- Registry

  def registerJob(jobSpec: JobSpec): ClientIO[ValidatedNel[QuckooError, JobId]] = {
    // The Scala compiler needs some help resolving this decoder
    implicit lazy val resultDec: Decoder[ValidatedNel[QuckooError, JobId]] =
      Decoder[ValidatedNel[QuckooError, JobId]]

    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI").body(jobSpec).response(asJson[ValidatedNel[QuckooError, JobId]])
      )
      result <- ClientIO.handleAttempt(request)
    } yield result
  }

  def fetchJob(jobId: JobId): ClientIO[Option[JobSpec]] =
    for {
      request <- ClientIO.auth.map(_.get(uri"$JobsURI/$jobId").response(asJson[JobSpec]))
      result  <- ClientIO.handleNotFoundOption(request)
    } yield result

  def fetchJobs(): ClientIO[List[(JobId, JobSpec)]] =
    for {
      request <- ClientIO.auth.map(_.get(uri"$JobsURI").response(asJson[List[(JobId, JobSpec)]]))
      result  <- ClientIO.handleAttempt(request)
    } yield result

  def enableJob(jobId: JobId): ClientIO[Unit] =
    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI/$jobId/enable").response(asJson[JobEnabled])
      )
      response <- ClientIO.fromFuture(request.send())
      _        <- if (response.code == 404) ClientIO.raiseError(JobNotFound(jobId)) else ClientIO.unit
    } yield ()

  def disableJob(jobId: JobId): ClientIO[Unit] =
    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI/$jobId/disable").response(asJson[JobDisabled])
      )
      response <- ClientIO.fromFuture(request.send())
      _        <- if (response.code == 404) ClientIO.raiseError(JobNotFound(jobId)) else ClientIO.unit
    } yield ()

  // -- Scheduler

  def startPlan(schedule: ScheduleJob): ClientIO[PlanId] =
    for {
      request <- ClientIO.auth.map(
        _.put(uri"$ExecutionPlansURI").body(schedule).response(asJson[PlanId])
      )
      response <- ClientIO.fromFuture(request.send())
      body <- if (response.code == 404) ClientIO.raiseError(JobNotFound(schedule.jobId))
      else ClientIO.fromEither(response.body)
      result <- ClientIO.fromAttempt(body)
    } yield result

  def cancelPlan(planId: PlanId): ClientIO[Unit] =
    for {
      request  <- ClientIO.auth.map(_.delete(uri"$ExecutionPlansURI/$planId"))
      response <- ClientIO.fromFuture(request.send())
      _ <- if (response.code == 404) ClientIO.raiseError(ExecutionPlanNotFound(planId))
      else ClientIO.unit
    } yield ()

  def fetchPlans(): ClientIO[List[(PlanId, ExecutionPlan)]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$ExecutionPlansURI").response(asJson[List[(PlanId, ExecutionPlan)]])
      )
      result <- ClientIO.handleAttempt(request)
    } yield result

  def fetchPlan(planId: PlanId): ClientIO[Option[ExecutionPlan]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$ExecutionPlansURI/$planId").response(asJson[ExecutionPlan])
      )
      result <- ClientIO.handleNotFoundOption(request)
    } yield result

  def fetchTasks(): ClientIO[List[(TaskId, TaskExecution)]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$TaskExecutionsURI").response(asJson[List[(TaskId, TaskExecution)]])
      )
      result <- ClientIO.handleAttempt(request)
    } yield result

  def fetchTask(taskId: TaskId): ClientIO[Option[TaskExecution]] =
    for {
      request <- ClientIO.auth.map(
        _.get(uri"$TaskExecutionsURI/$taskId").response(asJson[TaskExecution])
      )
      result <- ClientIO.handleNotFoundOption(request)
    } yield result

}
