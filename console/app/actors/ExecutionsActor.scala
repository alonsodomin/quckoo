package actors

import akka.actor._
import akka.contrib.pattern.ClusterClient.{Send, SendToAll}
import akka.pattern._
import akka.util.Timeout
import common.MessageTypes
import io.chronos.protocol.{ListenerProtocol, _}
import io.chronos.{Execution, path}
import model.ExecutionModel
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 12/07/15.
 */
object ExecutionsActor {
  import MessageTypes._

  def props(websocket: ActorRef): Props =
    Props(classOf[ExecutionsActor], websocket)

  sealed trait SubscriptionEvent
  
  case class Notification(executionId: String, status: String) extends SubscriptionEvent

  sealed abstract class Message(val messageType: MessageType, val payload: Any)

  case object GetFinishedExecutions extends Message(Request, Nil)
  case class ExecutionNotification(execution: ExecutionModel) extends Message(Event, execution)

  object SubscriptionEvent {
    implicit def subscriptionEventFormat: Format[SubscriptionEvent] = Format(
      (__ \ "event").read[String].flatMap {
        case "notification" => Notification.notificationFormat.map(identity)
        case other => Reads(_ => JsError("Unknown client event: " + other))
      },
      Writes {
        case n: Notification => Notification.notificationFormat.writes(n)
      }
    )

    implicit def subscriptionEventFormatter: FrameFormatter[SubscriptionEvent] = FrameFormatter.jsonFrame.transform(
      clientEvent => Json.toJson(clientEvent),
      json => Json.fromJson[SubscriptionEvent](json).fold(
        invalid => throw new RuntimeException("Bad client event on WebSocket: " + invalid),
        valid => valid
      )
    )
  }

  object Notification {
    implicit def notificationFormat: Format[Notification] = (
        (__ \ "event").format[String] and
        (__ \ "executionId").format[String] and
        (__ \ "status").format[String]
    )({
      case ("execution", executionId, status) => Notification(executionId, status)
    },
      notif => ("execution", notif.executionId, notif.status)
    )

  }

}

class ExecutionsActor(upstream: ActorRef) extends Actor with ActorLogging {
  import Execution._
  import ExecutionsActor._
  import SchedulerProtocol._
  import context.dispatcher

  private val chronosClient = context.actorSelection(context.system / "chronosClient")

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    chronosClient ! Send(path.ExecutionMonitor, ListenerProtocol.Subscribe, localAffinity = false)
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    chronosClient ! Send(path.ExecutionMonitor, ListenerProtocol.Unsubscribe, localAffinity = false)
  }

  def receive = {
    case ListenerProtocol.SubscribeAck =>
      log.info("Subscribed to executions.")
      context.become(ready, discardOld = false)
  }

  def ready: Receive = {
    case GetFinishedExecutions =>
      implicit val timeout = Timeout(10.seconds)
      val req = GetExecutions(_ is Done)
      ( chronosClient ? SendToAll(path.Scheduler, req)) map {
        case e: Execution => ExecutionModel(e.executionId.toString(), e.stage.toString)
      } pipeTo sender()
    
    case ExecutionEvent(executionId, status) =>
      val execution = ExecutionModel(executionId.toString(), status.toString)
      log.info("Received execution: " + execution)
      upstream ! execution

    case ListenerProtocol.UnsubscribeAck =>
      context.unbecome()
  }

}
