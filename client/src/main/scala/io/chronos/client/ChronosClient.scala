package io.chronos.client

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.client.ClusterClient.Send
import akka.pattern._
import akka.util.Timeout
import io.chronos.protocol.RegistryProtocol

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object ChronosClient {

  final val RegistryPath = "/user/scheduler/registry"

}

class ChronosClient(clusterClient: ActorRef) extends Actor with ActorLogging {
  import ChronosClient._
  import RegistryProtocol._
  import context.dispatcher

  def receive: Receive = {
    case cmd: RegistryCommand =>
      implicit val timeout = Timeout(5 seconds)
      (clusterClient ? Send(RegistryPath, cmd, localAffinity = true)) pipeTo sender()
  }

}
