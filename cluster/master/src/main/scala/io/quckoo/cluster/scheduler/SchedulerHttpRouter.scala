package io.quckoo.cluster.scheduler

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import de.heikoseeberger.akkahttpupickle.UpickleSupport

import io.quckoo.api.{Scheduler => SchedulerApi}
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization

/**
  * Created by domingueza on 21/03/16.
  */
trait SchedulerHttpRouter extends UpickleSupport { this: SchedulerApi =>

  import StatusCodes._
  import serialization.json.jvm._

  def schedulerApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    pathPrefix("plans") {
      pathEnd {
        get {
          extractExecutionContext { implicit ec =>
            complete(allExecutionPlanIds)
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
        }
      }
    }

}
