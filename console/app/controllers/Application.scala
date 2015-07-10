package controllers

import javax.inject.Inject

import components.RemoteScheduler
import play.api.mvc._
import play.libs.Json

import scala.concurrent.ExecutionContext

class Application @Inject() (val remoteScheduler: RemoteScheduler) extends Controller {

  def index = Action {
    Ok(views.html.index("Scheduled Jobs"))
  }

  def scheduledJobs = Action.async {
    implicit val xc: ExecutionContext = ExecutionContext.global
    remoteScheduler.jobDefinitions.map(specs => Ok(Json.toJson(specs).asText()))
  }

}
