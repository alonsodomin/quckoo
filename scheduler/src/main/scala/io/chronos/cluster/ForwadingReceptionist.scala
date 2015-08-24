package io.chronos.cluster

import akka.actor.{Actor, ActorRef}
import akka.cluster.client.ClusterClientReceptionist

/**
 * Created by aalonsodominguez on 24/08/15.
 */
class ForwadingReceptionist(actorRef: ActorRef) extends Actor {

  ClusterClientReceptionist(context.system).registerService(self)

  def receive: Receive = {
    case msg: Any =>
      actorRef.tell(msg, sender())
  }

}
