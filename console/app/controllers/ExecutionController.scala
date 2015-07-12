package controllers

import javax.inject.Inject

import actors.ExecutionSubscriberActor
import components.ChronosClient
import io.chronos.Execution
import play.api.Play.current
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller, WebSocket}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class ExecutionController @Inject() (private val client: ChronosClient)
  extends Controller {

  import ExecutionSubscriberActor.SubscriptionEvent._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  val subscriptionId = 2

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

  def executionsWs = WebSocket.acceptWithActor { _ => websocket =>
    ExecutionSubscriberActor.props(websocket)
  }

}
