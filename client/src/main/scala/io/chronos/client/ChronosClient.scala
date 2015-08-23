package io.chronos.client

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.client.ClusterClient.Send
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object ChronosClient {

  final val RegistryPath = "/user/registry"
  final val SchedulerPath = "/user/scheduler"

  def props(clientSettings: ClusterClientSettings) =
    Props(classOf[ChronosClient], clientSettings)

}

class ChronosClient(clientSettings: ClusterClientSettings)
  extends Actor with ActorLogging {

  import ChronosClient._
  import RegistryProtocol._
  import SchedulerProtocol._

  private val clusterClient = context.actorOf(ClusterClient.props(clientSettings), "client")

  def receive: Receive = {
    case cmd: RegistryCommand =>
      val handler = context.actorOf(Props(classOf[RequestHandler], sender()))
      clusterClient.tell(Send(RegistryPath, cmd, localAffinity = true), handler)

    case cmd: SchedulerCommand =>
      val handler = context.actorOf(Props(classOf[RequestHandler], sender()))
      clusterClient.tell(Send(SchedulerPath, cmd, localAffinity = true), handler)
  }

}

private class RequestHandler(replyTo: ActorRef) extends Actor {

  def receive: Receive = {
    case msg: Any =>
      replyTo ! msg
      context.stop(self)
  }

}