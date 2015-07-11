package controllers

import javax.inject.Inject

import components.ChronosClient
import io.chronos.Execution
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class ExecutionController @Inject() (private val client: ChronosClient) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action {
    Ok(views.html.executions("Executions"))
  }

  def executions = Action.async {
    client.executions.map { executions =>
      def writer(exec: Execution): JsValue = Json.obj(
        "id"     -> exec.executionId.toString,
        "status" -> exec.status.toString
      )
      implicit val writes = Writes(writer)
      Ok(Json.toJson(executions))
    }
  }

}
