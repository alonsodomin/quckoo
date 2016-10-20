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

package io.quckoo.cluster.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent}
import io.quckoo.api.{EventDef, RequestTimeoutHeader}
import io.quckoo.cluster.core.QuckooServer
import io.quckoo.cluster.registry.RegistryHttpRouter
import io.quckoo.cluster.scheduler.SchedulerHttpRouter
import io.quckoo.protocol.Event
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.serialization.json._
import io.quckoo.util.Attempt

import scala.concurrent.duration._

trait HttpRouter
    extends StaticResources with RegistryHttpRouter with SchedulerHttpRouter with AuthDirectives
    with EventStream with EventStreamMarshalling { this: QuckooServer =>

  import StatusCodes._

  private[this] val DefaultTimeout = 2500 millis

  private[this] def extractTimeout: Directive1[FiniteDuration] = {
    optionalHeaderValueByName(RequestTimeoutHeader).map(_.flatMap { timeoutValue =>
      LawfulTry(timeoutValue.toLong).map(_ millis).toOption
    } getOrElse DefaultTimeout)
  }

  private[this] def defineApi(implicit system: ActorSystem,
                              materializer: ActorMaterializer): Route =
    extractTimeout { implicit timeout =>
      pathPrefix("auth") {
        path("login") {
          post {
            authenticateUser
          }
        } ~ path("refresh") {
          get {
            refreshPassport
          }
        }
      } ~ authenticated { implicit passport =>
        path("auth" / "logout") {
          post {
            invalidateAuth {
              complete(OK)
            }
          }
        } ~ pathPrefix("cluster") {
          get {
            pathEnd {
              extractExecutionContext { implicit ec =>
                complete(clusterState)
              }
              /*} ~ path("master") {
              complete(asSSE(masterEvents, "master"))
            } ~ path("worker") {
              complete(asSSE(workerEvents, "worker"))*/
            }
          }
        } ~ pathPrefix("registry") {
          registryApi
        } ~ pathPrefix("scheduler") {
          schedulerApi
        }
      }
    }

  private[this] def exceptionHandler(log: LoggingAdapter) = ExceptionHandler {
    case exception =>
      extractUri { uri =>
        log.error(exception, s"Request to URI '$uri' threw exception.")
        complete(HttpResponse(InternalServerError, entity = exception.getMessage))
      }
  }

  private[this] def rejectionHandler(log: LoggingAdapter) =
    RejectionHandler.newBuilder().handle {
      case ValidationRejection(msg, cause) =>
        log.error(s"$msg - reason: $cause")
        complete(HttpResponse(BadRequest, entity = msg))
    } result ()

  def router(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    logRequest("HTTPRequest") {
      logResult("HTTPResponse") {
        handleExceptions(exceptionHandler(system.log)) {
          handleRejections(rejectionHandler(system.log)) {
            pathPrefix("api") {
              defineApi
            } ~ path("events") {
              complete(eventBus)
            } ~ staticResources
          }
        }
      }
    }

}
