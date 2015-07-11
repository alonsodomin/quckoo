package controllers

import javax.inject.Inject

import components.ChronosClient
import io.chronos.JobSchedule
import io.chronos.id.ScheduleId
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class SchedulerController @Inject() (val client: ChronosClient) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action {
    Ok(views.html.scheduler("Scheduler"))
  }

  def schedules = Action.async {
    client.scheduledJobs.map { jobs =>
      def writer(pair: (ScheduleId, JobSchedule)): JsValue = Json.obj(
        "id"      -> pair._1.toString,
        "trigger" -> pair._2.trigger.getClass.getSimpleName
      )
      implicit val writes = Writes(writer)
      Ok(Json.toJson(jobs))
    }
  }

}
