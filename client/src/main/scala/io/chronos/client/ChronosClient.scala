package io.chronos.client

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.client.ClusterClient.Send
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.pattern._
import akka.util.Timeout
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object ChronosClient {

  final val RegistryPath = "/user/registry"
  final val SchedulerPath = "/user/scheduler"

  final val DefaultTimeout = 5 seconds

  def props(clientSettings: ClusterClientSettings,
            registryTimeout: FiniteDuration = DefaultTimeout,
            schedulerTimeout: FiniteDuration = DefaultTimeout) =
    Props(classOf[ChronosClient], clientSettings, registryTimeout, schedulerTimeout)

}

class ChronosClient(clientSettings: ClusterClientSettings, registryTimeout: FiniteDuration, schedulerTimeout: FiniteDuration)
  extends Actor with ActorLogging {

  import ChronosClient._
  import RegistryProtocol._
  import SchedulerProtocol._
  import context.dispatcher

  private val clusterClient = context.actorOf(ClusterClient.props(clientSettings))

  def receive: Receive = {
    case cmd: RegistryCommand =>
      implicit val timeout = Timeout(registryTimeout)
      (clusterClient ? Send(RegistryPath, cmd, localAffinity = true)) pipeTo sender()

    case cmd: SchedulerCommand =>
      implicit val timeout = Timeout(schedulerTimeout)
      (clusterClient ? Send(SchedulerPath, cmd, localAffinity = true)) pipeTo sender()
  }

}
