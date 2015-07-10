package controllers

import javax.inject.Inject

import components.RemoteScheduler
import io.chronos.JobSpec
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext

class Application @Inject() (val remoteScheduler: RemoteScheduler) extends Controller {

  def index = Action {
    Ok(views.html.index("Available Jobs"))
  }

  def scheduledJobs = Action.async {
    implicit val xc: ExecutionContext = ExecutionContext.global
    remoteScheduler.jobDefinitions.map { specs =>
      def writer(spec: JobSpec): JsValue = Json.obj(
        "id" -> spec.id,
        "displayName" -> spec.displayName,
        "description" -> spec.description
      )
      implicit val writes = Writes(writer)
      Ok(Json.toJson(specs))
    }
  }

}
