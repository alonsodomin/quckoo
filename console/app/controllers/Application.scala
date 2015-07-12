package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

class Application @Inject() extends Controller {

  def index = Action {
    Ok(views.html.index("Cluster overview"))
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(JavaScriptReverseRouter("jsRoutes")(
      routes.javascript.JobRepositoryController.jobs,
      routes.javascript.SchedulerController.schedules,
      routes.javascript.ExecutionController.executionsWs
    )).as("text/javascript")
  }

}
