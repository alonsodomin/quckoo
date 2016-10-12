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

package io.quckoo.cluster.scheduler

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import de.heikoseeberger.akkahttpupickle.UpickleSupport
import de.heikoseeberger.akkasse.EventStreamMarshalling

import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.auth.Passport
import io.quckoo.fault._
import io.quckoo.cluster.http._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization.json._

import scala.concurrent.duration._

import scalaz._

/**
  * Created by domingueza on 21/03/16.
  */
trait SchedulerHttpRouter extends UpickleSupport with EventStreamMarshalling {
  this: SchedulerApi with SchedulerStreams =>

  import StatusCodes._

  def schedulerApi(
      implicit system: ActorSystem,
      materializer: ActorMaterializer,
      timeout: FiniteDuration,
      passport: Passport
  ): Route =
    pathPrefix("plans") {
      pathEnd {
        get {
          extractExecutionContext { implicit ec =>
            complete(executionPlans)
          }
        } ~ post {
          entity(as[ScheduleJob]) { req =>
            extractExecutionContext { implicit ec =>
              onSuccess(scheduleJob(req)) {
                case \/-(res)                  => complete(res)
                case -\/(JobNotEnabled(jobId)) => complete(BadRequest -> jobId)
                case -\/(JobNotFound(jobId))   => complete(NotFound -> jobId)
                case -\/(error)                => complete(InternalServerError -> error)
              }
            }
          }
        }
      } ~ path(JavaUUID) { planId =>
        get {
          extractExecutionContext { implicit ec =>
            onSuccess(executionPlan(planId)) {
              case Some(plan) => complete(plan)
              case _          => complete(NotFound -> planId)
            }
          }
        } ~ delete {
          extractExecutionContext { implicit ec =>
            onSuccess(cancelPlan(planId)) {
              case \/-(res)                      => complete(res)
              case -\/(ExecutionPlanNotFound(_)) => complete(NotFound -> planId)
            }
          }
        }
      }
    } ~ pathPrefix("executions") {
      pathEnd {
        get {
          extractExecutionContext { implicit ec =>
            complete(executions)
          }
        }
      } ~ path(JavaUUID) { taskId =>
        get {
          extractExecutionContext { implicit ec =>
            onSuccess(execution(taskId)) {
              case Some(task) => complete(task)
              case _          => complete(NotFound -> taskId)
            }
          }
        }
      }
    } ~ path("events") {
      get {
        complete(asSSE(schedulerEvents, "scheduler"))
      }
    }

}
