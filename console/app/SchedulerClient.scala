import akka.actor.{Actor, ActorSelection, Props}
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern._
import akka.util.Timeout
import io.chronos.path
import io.chronos.protocol.SchedulerProtocol._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 09/07/15.
 */
object SchedulerClient {

  case object NoAnswer

  def props(initialContacts: Set[ActorSelection]): Props =
    Props(classOf[SchedulerClient], initialContacts)
}

class SchedulerClient(val initialContacts: Set[ActorSelection]) extends Actor {
  import SchedulerClient._

  val client = context.actorOf(ClusterClient.props(initialContacts), "schedulerClient")

  override def receive: Receive = {
    case req: SchedulerRequest =>
      implicit val xc: ExecutionContext = ExecutionContext.global
      implicit val timeout = Timeout(5.seconds)

      (client ? SendToAll(path.Scheduler, req)) map {
        case res: SchedulerResponse => res
      } recover {
        case _ => NoAnswer
      } pipeTo sender()
  }

}
