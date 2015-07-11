package controllers

import javax.inject.Inject

import components.RemoteScheduler
import io.chronos.JobSpec
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class JobRepository @Inject() (val remoteScheduler: RemoteScheduler) extends Controller {

  def index = Action {
    Ok(views.html.repository("Job Repository"))
  }

  def jobs = Action.async {
    implicit val xc: ExecutionContext = ExecutionContext.global
    remoteScheduler.jobDefinitions.map { specs =>
      def writer(spec: JobSpec): JsValue = Json.obj(
        "id" -> spec.id,
        "displayName" -> spec.displayName,
        "description" -> spec.description,
        "jobClass"    -> spec.jobClass.getName
      )
      implicit val writes = Writes(writer)
      Ok(Json.toJson(specs))
    }
  }

}
