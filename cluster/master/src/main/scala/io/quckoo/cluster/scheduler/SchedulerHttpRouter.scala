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

package io.quckoo.cluster.scheduler

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer

import cats.effect.IO
import cats.implicits._

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import de.heikoseeberger.akkasse.scaladsl.marshalling.EventStreamMarshalling

import io.circe.generic.auto._

import io.quckoo._
import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.auth.Passport
import io.quckoo.cluster.http._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.json._

/**
  * Created by domingueza on 21/03/16.
  */
trait SchedulerHttpRouter extends EventStreamMarshalling {
  api: SchedulerApi[IO] with SchedulerStreams =>

  import StatusCodes._
  import TimeoutDirectives._
  import ErrorAccumulatingCirceSupport._

  def schedulerApi(
      implicit
      system: ActorSystem,
      materializer: Materializer,
      passport: Passport
  ): Route =
    extractTimeout(DefaultTimeout) { implicit timeout =>
      pathPrefix("plans") {
        pathEnd {
          get {
            extractExecutionContext { implicit ec =>
              complete(api.fetchPlans().unsafeToFuture())
            }
          } ~ put {
            entity(as[ScheduleJob]) { req =>
              extractExecutionContext { implicit ec =>
                api.startPlan(req)
              }
            }
          }
        } ~ path(JavaUUID) { planUUID =>
          get {
            extractExecutionContext { implicit ec =>
              onSuccess(api.fetchPlan(PlanId(planUUID)).unsafeToFuture()) {
                case Some(plan) => complete(plan)
                case _          => complete(NotFound -> planUUID)
              }
            }
          } ~ delete {
            extractExecutionContext { implicit ec =>
              api.cancelPlan(PlanId(planUUID))
            }
          }
        }
      } ~ pathPrefix("executions") {
        pathEnd {
          get {
            extractExecutionContext { implicit ec =>
              api.fetchTasks()
            }
          }
        } ~ path(JavaUUID) { taskUUID =>
          get {
            extractExecutionContext { implicit ec =>
              onSuccess(api.fetchTask(TaskId(taskUUID)).unsafeToFuture()) {
                case Some(task) => complete(task)
                case _          => complete(NotFound -> taskUUID)
              }
            }
          }
        }
      }
    }

  def schedulerEvents(implicit system: ActorSystem, materializer: Materializer): Route =
    path("scheduler") {
      get {
        complete(asSSE(schedulerTopic))
      }
    }

}
