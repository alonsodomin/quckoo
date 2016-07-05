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
import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent}
import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.cluster.http._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization

/**
  * Created by domingueza on 21/03/16.
  */
trait SchedulerHttpRouter extends UpickleSupport with EventStreamMarshalling {
  this: SchedulerApi with SchedulerStreams =>

  import StatusCodes._
  import upickle.default._
  import serialization.json.jvm._

  def schedulerApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    pathPrefix("plans") {
      pathEnd {
        get {
          extractExecutionContext { implicit ec =>
            complete(executionPlans)
          }
        } ~ post {
          entity(as[ScheduleJob]) { req =>
            extractExecutionContext { implicit ec =>
              onSuccess(schedule(req)) {
                case Left(notFound) => complete(NotFound -> notFound.jobId)
                case Right(plan)    => complete(plan)
              }
            }
          }
        }
      } ~ path(JavaUUID) { planId =>
        get {
          extractExecutionContext { implicit ec =>
            onSuccess(executionPlan(planId)) {
              case Some(plan) => complete(plan)
              case _          => complete(NotFound)
            }
          }
        } ~ delete {
          extractExecutionContext { implicit ec =>
            complete(cancelPlan(planId))
          }
        }
      }
    } ~ pathPrefix("tasks") {
      pathEnd {
        get {
          extractExecutionContext { implicit ec =>
            complete(tasks)
          }
        }
      } ~ path(JavaUUID) { taskId =>
        get {
          extractExecutionContext { implicit ec =>
            onSuccess(task(taskId)) {
              case Some(task) => complete(task)
              case _          => complete(NotFound)
            }
          }
        }
      }
    } ~ path("queue") {
      get {
        complete(asSSE(queueMetrics, "metrics"))
      }
    }

}
