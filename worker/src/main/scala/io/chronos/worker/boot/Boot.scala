package io.chronos.worker.boot

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyModuleResolver}
import io.chronos.worker.{JobExecutor, Worker}

/**
 * Created by domingueza on 09/07/15.
 */
object Boot {

  val DefaultPort = 0

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + DefaultPort)
      .withFallback(ConfigFactory.load("worker"))
    val system = ActorSystem("ChronosWorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "user" / "receptionist"
    }.toSet

    val ivyConfig = IvyConfiguration(conf)
    val moduleResolver = new IvyModuleResolver(ivyConfig)

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val clusterClient = system.actorOf(ClusterClient.props(clientSettings), "clusterClient")
    system.actorOf(Worker.props(clusterClient, JobExecutor.props(moduleResolver)), "worker")
  }

}
