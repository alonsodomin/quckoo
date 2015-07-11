package controllers

import javax.inject.Inject

import components.RemoteScheduler
import io.chronos.JobSchedule
import io.chronos.id.ScheduleId
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class SchedulerController @Inject() (val remoteScheduler: RemoteScheduler) extends Controller {

  def index = Action {
    Ok(views.html.scheduler("Scheduler"))
  }

  def schedules = Action.async {
    implicit val xc: ExecutionContext = ExecutionContext.global
    remoteScheduler.scheduledJobs map { schedulePairs =>
      def writer(pair: (ScheduleId, JobSchedule)): JsValue = Json.obj(
        "id"      -> pair._1.toString,
        "trigger" -> pair._2.trigger.getClass.getSimpleName
      )
      implicit val writes = Writes(writer)
      Ok(Json.toJson(schedulePairs))
    }
  }

}
