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

package io.quckoo.client.http.akka

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}

import cats.effect.IO

import io.circe.generic.auto._

import io.quckoo.api2.Registry
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}
import io.quckoo.serialization.json._
import io.quckoo.{JobId, JobNotFound, JobSpec}

import scala.concurrent.duration._

trait AkkaHttpRegistry extends AkkaHttpClientSupport with Registry[ClientIO] {

  override def enableJob(
      jobId: JobId): ClientIO[Either[JobNotFound, JobEnabled]] =
    ClientIO { session =>
      val request =
        HttpRequest(HttpMethods.PUT, uri = s"$JobsURI/$jobId/enable")
          .withSession(session)

      sendRequest(request) {
        case response if response.status == StatusCodes.OK =>
          IO.pure(Right(JobEnabled(jobId)))
        case response if response.status == StatusCodes.NotFound =>
          IO.pure(Left(JobNotFound(jobId)))
      }
    }

  override def disableJob(
      jobId: JobId): ClientIO[Either[JobNotFound, JobDisabled]] =
    ClientIO { session =>
      val request =
        HttpRequest(HttpMethods.PUT, uri = s"$JobsURI/$jobId/disable")
          .withSession(session)

      sendRequest(request) {
        case response if response.status == StatusCodes.OK =>
          IO.pure(Right(JobDisabled(jobId)))
        case response if response.status == StatusCodes.NotFound =>
          IO.pure(Left(JobNotFound(jobId)))
      }
    }

  override def fetchJob(jobId: JobId): ClientIO[Option[JobSpec]] =
    ClientIO { session =>
      def notFoundHandler: HttpResponseHandler[Option[JobSpec]] = {
        case res if res.status == StatusCodes.NotFound => IO.pure(None)
      }
      val handler = parseEntity[JobSpec](500 millis)
        .andThen(_.map(Some(_)))
        .orElse(notFoundHandler)

      val request = HttpRequest(HttpMethods.GET, uri = s"$JobsURI/$jobId")
        .withSession(session)
      sendRequest(request)(handler)
    }

  override def registerJob(jobSpec: JobSpec) = ???
}
