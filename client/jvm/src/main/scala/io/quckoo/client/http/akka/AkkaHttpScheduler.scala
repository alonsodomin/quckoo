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
import io.quckoo.{ExecutionPlanNotFound, JobId, PlanId, Trigger}
import io.quckoo.api2.Scheduler
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.protocol.scheduler.ExecutionPlanCancelled

import scala.concurrent.duration._

trait AkkaHttpScheduler extends AkkaHttpClientSupport with Scheduler[ClientIO] {

  override def cancelPlan(planId: PlanId)
    : ClientIO[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] =
    ClientIO.auth { session =>
      val request = HttpRequest(HttpMethods.DELETE, uri = s"$ExecutionPlansURI/$planId").withSession(session)

      sendRequest(request) {
        case res if res.status == StatusCodes.NotFound =>
          IO.pure(Left(ExecutionPlanNotFound(planId)))

        case res if res.status == StatusCodes.OK =>
          parseEntity[ExecutionPlanCancelled](500 millis)
            .andThen(_.map(Right(_)))
            .apply(res)
      }
    }

  override def submit(jobId: JobId, trigger: Trigger, timeout: Option[FiniteDuration]) = ???
}
