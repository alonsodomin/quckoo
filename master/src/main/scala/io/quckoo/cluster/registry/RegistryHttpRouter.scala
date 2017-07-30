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

package io.quckoo.cluster.registry

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import de.heikoseeberger.akkasse.scaladsl.marshalling.EventStreamMarshalling

import io.circe.generic.auto._

import io.quckoo._
import io.quckoo.api.{Registry => RegistryApi}
import io.quckoo.auth.Passport
import io.quckoo.cluster.http._
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}

import scala.concurrent.duration._

/**
  * Created by domingueza on 21/03/16.
  */
trait RegistryHttpRouter extends EventStreamMarshalling {
  this: RegistryApi with RegistryStreams =>

  import StatusCodes._
  import TimeoutDirectives._
  import ErrorAccumulatingCirceSupport._

  def registryApi(implicit system: ActorSystem,
                  materializer: Materializer,
                  passport: Passport): Route =
    pathPrefix("jobs") {
      pathEnd {
        get {
          extractTimeout(DefaultTimeout) { implicit timeout =>
            extractExecutionContext { implicit ec =>
              complete(fetchJobs)
            }
          }
        } ~ put {
          extractTimeout(10 minutes) { implicit timeout =>
            entity(as[JobSpec]) { jobSpec =>
              extractExecutionContext { implicit ec =>
                complete(registerJob(jobSpec))
              }
            }
          }
        }
      } ~ pathPrefix(Segment) { jobId =>
        extractTimeout(DefaultTimeout) { implicit timeout =>
          pathEnd {
            get {
              extractExecutionContext { implicit ec =>
                onSuccess(fetchJob(JobId(jobId))) {
                  case Some(spec) => complete(spec)
                  case _          => complete(NotFound)
                }
              }
            }
          } ~ path("enable") {
            post {
              extractExecutionContext { implicit ec =>
                onSuccess(enableJob(JobId(jobId))) {
                  case Right(res @ JobEnabled(_)) => complete(res)
                  case Left(JobNotFound(_))       => complete(NotFound -> jobId)
                }
              }
            }
          } ~ path("disable") {
            post {
              extractExecutionContext { implicit ec =>
                onSuccess(disableJob(JobId(jobId))) {
                  case Right(res @ JobDisabled(_)) => complete(res)
                  case Left(JobNotFound(_))        => complete(NotFound -> jobId)
                }
              }
            }
          }
        }
      }
    }

  def registryEvents(implicit system: ActorSystem,
                     aterializer: Materializer): Route =
    path("registry") {
      get {
        complete(asSSE(registryTopic))
      }
    }

}
