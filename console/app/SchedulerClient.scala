import akka.actor.{Actor, ActorSelection, Props}
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern._
import io.chronos.path
import io.chronos.protocol.SchedulerProtocol._

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
  import context.dispatcher

  val chronosClient = context.actorOf(ClusterClient.props(initialContacts), "schedulerClient")

  override def receive: Receive = {
    case req: SchedulerRequest =>
      (chronosClient ? SendToAll(path.Scheduler, req)) map {
        case res: SchedulerResponse => res
      } recover {
        case _ => NoAnswer
      } pipeTo sender()
  }

}
