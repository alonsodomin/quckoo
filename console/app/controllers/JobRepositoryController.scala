package controllers

import javax.inject.Inject

import io.chronos.{JobRepository, JobSpec}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class JobRepositoryController @Inject() (val jobRepository: JobRepository) extends Controller {

  def index = Action {
    Ok(views.html.repository("Job Repository"))
  }

  def jobs = Action {
    def writer(spec: JobSpec): JsValue = Json.obj(
      "id"          -> spec.id,
      "displayName" -> spec.displayName,
      "description" -> spec.description,
      "jobClass"    -> spec.jobClass.getName
    )
    implicit val writes = Writes(writer)
    Ok(Json.toJson(jobRepository.availableSpecs))
  }

}
