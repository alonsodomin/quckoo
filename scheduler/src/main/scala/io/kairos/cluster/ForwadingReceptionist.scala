package io.kairos.cluster

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.client.ClusterClientReceptionist

/**
 * Created by aalonsodominguez on 24/08/15.
 */
class ForwadingReceptionist(actorRef: ActorRef) extends Actor with ActorLogging {

  ClusterClientReceptionist(context.system).registerService(self)

  def receive: Receive = {
    case msg: Any =>
      actorRef.tell(msg, sender())
  }

}
