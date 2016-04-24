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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import de.heikoseeberger.akkasse.EventStreamMarshalling
import io.quckoo.JobSpec
import io.quckoo.api.{Registry => RegistryApi}
import io.quckoo.id.JobId
import io.quckoo.cluster.http._
import io.quckoo.serialization

/**
  * Created by domingueza on 21/03/16.
  */
trait RegistryHttpRouter extends UpickleSupport with EventStreamMarshalling {
  this: RegistryApi with RegistryStreams =>

  import StatusCodes._
  import upickle.default._
  import serialization.json.jvm._

  def registryApi(implicit system: ActorSystem, materializer: Materializer): Route =
    pathPrefix("jobs") {
      pathEnd {
        get {
          extractExecutionContext { implicit ec =>
            complete(fetchJobs)
          }
        } ~ put {
          entity(as[JobSpec]) { jobSpec =>
            extractExecutionContext { implicit ec =>
              complete(registerJob(jobSpec))
            }
          }
        }
      } ~ pathPrefix(JavaUUID) { jobId =>
        pathEnd {
          get {
            extractExecutionContext { implicit ec =>
              onSuccess(fetchJob(JobId(jobId))) {
                case Some(jobSpec) => complete(jobSpec)
                case _             => complete(NotFound)
              }
            }
          }
        } ~ path("enable") {
          post {
            extractExecutionContext { implicit ec =>
              complete(enableJob(JobId(jobId)))
            }
          }
        } ~ path("disable") {
          post {
            extractExecutionContext { implicit ec =>
              complete(disableJob(JobId(jobId)))
            }
          }
        }
      }
    } ~ path("events") {
      get {
        complete(asSSE(registryEvents, "registry"))
      }
    }



}
