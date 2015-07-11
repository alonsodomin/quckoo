package controllers

import javax.inject.Inject

import io.chronos.id.ScheduleId
import io.chronos.{ExecutionPlan, JobSchedule}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class SchedulerController @Inject() (val executionPlan: ExecutionPlan) extends Controller {

  def index = Action {
    Ok(views.html.scheduler("Scheduler"))
  }

  def schedules = Action {
    def writer(pair: (ScheduleId, JobSchedule)): JsValue = Json.obj(
      "id"      -> pair._1.toString,
      "trigger" -> pair._2.trigger.getClass.getSimpleName
    )
    implicit val writes = Writes(writer)
    Ok(Json.toJson(executionPlan.scheduledJobs))
  }

}
