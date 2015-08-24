package io.chronos.worker.boot

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyResolve, Resolver}
import io.chronos.worker.{JobExecutor, Worker}

/**
 * Created by domingueza on 09/07/15.
 */
object Boot {

  val DefaultPort = 0

  def main(args: Array[String]): Unit = {
    val defaultConf = ConfigFactory.load("reference.conf")
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + DefaultPort).
      withFallback(ConfigFactory.load()).
      withFallback(defaultConf)

    val system = ActorSystem("ChronosWorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("chronos.contact-points")).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val clusterClient  = system.actorOf(ClusterClient.props(clientSettings), "client")

    val ivyConfig  = IvyConfiguration(conf)
    val ivyResolve = new IvyResolve(ivyConfig)
    val resolver   = system.actorOf(Resolver.props(ivyResolve), "resolver")

    val jobExecutorProps = JobExecutor.props(resolver)
    system.actorOf(Worker.props(clusterClient, jobExecutorProps), "worker")
  }

}
