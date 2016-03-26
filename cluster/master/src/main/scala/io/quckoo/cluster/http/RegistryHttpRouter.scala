package io.quckoo.cluster.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.quckoo.api.Registry
import io.quckoo.auth.AuthInfo
import io.quckoo.id.JobId
import io.quckoo.{JobSpec, serialization}

/**
  * Created by domingueza on 21/03/16.
  */
trait RegistryHttpRouter extends UpickleSupport { this: Registry =>

  import StatusCodes._
  import serialization.json.jvm._

  def registryApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
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
    }

}
