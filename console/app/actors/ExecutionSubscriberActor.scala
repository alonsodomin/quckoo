package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.protocol.SchedulerProtocol._
import io.chronos.topic
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

/**
 * Created by aalonsodominguez on 12/07/15.
 */
object ExecutionSubscriberActor {
  
  def props(websocket: ActorRef): Props =
    Props(classOf[ExecutionSubscriberActor], websocket)

  sealed trait SubscriptionEvent
  
  case class Notification(executionId: String, status: String) extends SubscriptionEvent

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
      case ("notification", executionId, status) => Notification(executionId, status)
    },
      notif => ("notification", notif.executionId, notif.status)
    )

  }

}

class ExecutionSubscriberActor(websocket: ActorRef) extends Actor with ActorLogging {
  import ExecutionSubscriberActor._

  private val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topic.Executions, self)
  
  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
      context.become(ready, discardOld = false)
  }

  def ready: Receive = {
    case _: DistributedPubSubMediator.UnsubscribeAck =>
      context.unbecome()

    case ExecutionEvent(executionId, status) =>
      val event = Notification(executionId.toString(), status.toString)
      log.info("Received execution event: " + event)
      websocket ! event
  }

}
