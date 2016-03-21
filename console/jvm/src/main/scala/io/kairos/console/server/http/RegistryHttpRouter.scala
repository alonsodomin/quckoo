package io.kairos.console.server.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import de.heikoseeberger.akkahttpupickle.UpickleSupport

import io.kairos.{JobSpec, serialization}
import io.kairos.console.server.RegistryFacade
import io.kairos.id.JobId

/**
  * Created by domingueza on 21/03/16.
  */
trait RegistryHttpRouter extends UpickleSupport { this: RegistryFacade =>

  import StatusCodes._
  import serialization.json.jvm._

  def registryApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    pathPrefix("jobs") {
      pathEnd {
        get {
          complete(registeredJobs)
        } ~ put {
          entity(as[JobSpec]) { jobSpec =>
            complete(registerJob(jobSpec))
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
            complete(enableJob(JobId(jobId)))
          }
        } ~ path("disable") {
          post {
            complete(disableJob(JobId(jobId)))
          }
        }
      }
    }

}
