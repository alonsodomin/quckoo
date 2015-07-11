package controllers

import javax.inject.Inject

import components.ChronosClient
import io.chronos.JobSpec
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class JobRepositoryController @Inject() (val client: ChronosClient) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action {
    Ok(views.html.repository("Job Repository"))
  }

  def jobs = Action.async {
    client.availableJobSpecs.map { specs =>
      def writer(spec: JobSpec): JsValue = Json.obj(
        "id"          -> spec.id,
        "displayName" -> spec.displayName,
        "description" -> spec.description,
        "jobClass"    -> spec.jobClass.getName
      )
      implicit val writes = Writes(writer)
      Ok(Json.toJson(specs))
    }
  }

}
