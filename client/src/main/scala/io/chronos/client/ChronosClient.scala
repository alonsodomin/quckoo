package io.chronos.client

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.client.ClusterClient.Send
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.pattern._
import akka.util.Timeout
import io.chronos.protocol.RegistryProtocol

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object ChronosClient {

  final val RegistryPath = "/user/scheduler/registry"

  def props(clientSettings: ClusterClientSettings) =
    Props(classOf[ChronosClient], clientSettings)

}

class ChronosClient(clientSettings: ClusterClientSettings) extends Actor with ActorLogging {
  import ChronosClient._
  import RegistryProtocol._
  import context.dispatcher

  private val clusterClient = context.actorOf(ClusterClient.props(clientSettings))

  def receive: Receive = {
    case cmd: RegistryCommand =>
      implicit val timeout = Timeout(5 seconds)
      (clusterClient ? Send(RegistryPath, cmd, localAffinity = true)) pipeTo sender()
  }

}
