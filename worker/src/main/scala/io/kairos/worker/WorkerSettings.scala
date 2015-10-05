package io.kairos.worker

import akka.actor.{ActorSystem, AddressFromURIString, Props, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.japi.Util._
import io.kairos.resolver.IvyConfiguration

import scala.concurrent.duration._

/**
 * Created by domingueza on 30/08/15.
 */
object WorkerSettings {

  final val DefaultRegisterFrequency = 10 seconds
  final val DefaultQueueAckTimeout = 5 seconds

  def apply(system: ActorSystem): WorkerSettings = {
    val config = system.settings.config.getConfig("chronos")
    val initialContacts = immutableSeq(config.getStringList("contact-points")).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    WorkerSettings(IvyConfiguration(config), ClusterClient.props(clientSettings), 10 minutes, 10 seconds)
  }

}

case class WorkerSettings private (ivyConfiguration: IvyConfiguration, clientProps: Props,
                                   registerInterval: FiniteDuration, queueAckTimeout: FiniteDuration)
