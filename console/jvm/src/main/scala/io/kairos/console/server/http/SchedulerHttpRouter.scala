package io.kairos.console.server.http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import de.heikoseeberger.akkahttpupickle.UpickleSupport

import io.kairos.console.server.core.SchedulerFacade
import io.kairos.protocol.SchedulerProtocol
import io.kairos.serialization

/**
  * Created by domingueza on 21/03/16.
  */
trait SchedulerHttpRouter extends UpickleSupport { this: SchedulerFacade =>

  import SchedulerProtocol._
  import serialization.json.jvm._

  def schedulerApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    pathPrefix("plans") {
      pathEnd {
        get {
          complete(allExecutionPlanIds)
        } ~ put {
          entity(as[ScheduleJob]) { sch =>
            complete(schedule(sch))
          }
        }
      } ~ path(JavaUUID) { planId =>
        get {
          complete(executionPlan(planId))
        }
      }
    }

}
