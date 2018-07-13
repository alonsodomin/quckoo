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

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.softwaremill.sttp.circe._

import io.circe.{Error => JsonError, _}

import io.quckoo._
import io.quckoo.client._
import io.quckoo.auth.{InvalidCredentials, Passport}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.json._

abstract class JVMQuckooClient(implicit backend: AkkaHttpBackend) extends NewQuckooClient {
  import NewQuckooClient._

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

  def enableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobEnabled]] =
    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI/$jobId/enable").response(asJson[Either[JobNotFound, JobEnabled]])
      )
      result <- ClientIO.handleNotFound(request)(JobNotFound(jobId), _.asRight[JobNotFound])
    } yield result

  def disableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobDisabled]] =
    for {
      request <- ClientIO.auth.map(
        _.post(uri"$JobsURI/$jobId/disable").response(asJson[Either[JobNotFound, JobDisabled]])
      )
      result <- ClientIO.handleNotFound(request)(JobNotFound(jobId), _.asRight[JobNotFound])
    } yield result

  // -- Scheduler

  //def startPlan(schedule: ScheduleJob): ClientIO[Either[JobNotFound, ExecutionPlanStarted]] = {
  //  import io.circe.generic.auto._
  //  for {
  //    request <- ClientIO.auth.map(
  //      _.put(uri"$ExecutionPlansURI")
  //        .body(schedule)
  //        .response(asJson[Either[JobNotFound, ExecutionPlanStarted]])
  //    )
  //    result <- ClientIO.handleAttempt(request)
  //  } yield result
  //}
}
