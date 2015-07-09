package controllers

import javax.inject.Inject

import components.RemoteScheduler
import play.api.mvc._

class Application @Inject() (val remoteScheduler: RemoteScheduler) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def scheduledJobs = Action.async {
    remoteScheduler.jobDefinitions.map(jobDefs => Ok(jobDefs))
  }

}
