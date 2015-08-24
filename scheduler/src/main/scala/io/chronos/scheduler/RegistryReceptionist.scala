package io.chronos.scheduler

import akka.actor.{Actor, ActorRef}
import akka.cluster.client.ClusterClientReceptionist
import io.chronos.protocol.RegistryProtocol.RegistryCommand

/**
 * Created by aalonsodominguez on 24/08/15.
 */
class RegistryReceptionist(registryRegion: ActorRef) extends Actor {

  ClusterClientReceptionist(context.system).registerService(self)

  def receive: Receive = {
    case msg: RegistryCommand =>
      registryRegion.tell(msg, sender())
  }

}
