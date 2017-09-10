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
import cats.implicits._

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import io.circe.generic.auto._
import io.circe.java8.time._

import io.quckoo._
import io.quckoo.api2.Scheduler
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.protocol.scheduler.{ExecutionPlanCancelled, ExecutionPlanStarted, ScheduleJob}
import io.quckoo.serialization.json._

import scala.concurrent.duration._

trait AkkaHttpScheduler extends AkkaHttpClientSupport with Scheduler[ClientIO] {
  import FailFastCirceSupport._

  override def cancelPlan(
      planId: PlanId
  ): ClientIO[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] =
    ClientIO.auth { session =>
      val request =
        HttpRequest(HttpMethods.DELETE, uri = s"$ExecutionPlansURI/$planId")
          .withSession(session)

      val successHandler =
        handleEntity[ExecutionPlanCancelled](_.status == StatusCodes.OK)
          .andThen(_.map(Right(_)))
      val notFoundHandler
        : HttpResponseHandler[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] = {
        case res if res.status == StatusCodes.NotFound =>
          IO.pure(Left(ExecutionPlanNotFound(planId)))
      }

      sendRequest(request)(successHandler.orElse(notFoundHandler))
    }

  override def submit(
      jobId: JobId,
      trigger: Trigger,
      timeout: Option[FiniteDuration]
  ): ClientIO[Either[InvalidJob, ExecutionPlanStarted]] = ClientIO.auth { session =>
    def request: IO[HttpRequest] = {
      val payload = ScheduleJob(jobId, trigger, timeout)
      marshalEntity(payload).map { entity =>
        HttpRequest(HttpMethods.PUT, uri = ExecutionPlansURI, entity = entity)
      }
    }

    def handler = {
      val successHandler = handleEntity[ExecutionPlanStarted](_.status == StatusCodes.OK)
        .andThen(_.map(Right(_)))
      val failureHandler = handleEntity[InvalidJob](_.status != StatusCodes.OK)
        .andThen(_.map(Left(_)))

      successHandler.orElse(failureHandler)
    }

    request >>= (sendRequest(_)(handler))
  }

  override def fetchPlan(planId: PlanId) = ???

  override def fetchTask(taskId: TaskId) = ???

  override def allPlans = ???

  override def allTasks = ???
}
