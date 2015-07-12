package controllers

import javax.inject.Inject

import components.{ChronosClient, ChronosListener}
import io.chronos.Execution
import io.chronos.protocol.SchedulerProtocol._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller, WebSocket}

/**
 * Created by aalonsodominguez on 11/07/15.
 */
class ExecutionController @Inject() (private val client: ChronosClient,
                                     private val listener: ChronosListener)
  extends Controller {

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

  def executionsWs = WebSocket.tryAccept[JsValue] { request =>
    listener.executions(subscriptionId).map { enum =>
      val in = Iteratee.foreach[JsValue](println).map { _ =>
        listener.unsubscribe(subscriptionId)
      }

      val out: Enumerator[JsValue] = enum.map { event =>
        def writer(event: ExecutionEvent): JsValue = Json.obj(
          "executionId" -> event.executionId.toString(),
          "status" -> event.status.toString
        )
        implicit val writes = Writes(writer)
        Json.toJson(event)
      }

      Right((in, out))
    }
  }

}
