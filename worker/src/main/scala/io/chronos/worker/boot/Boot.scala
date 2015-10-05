package io.chronos.worker.boot

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.japi.Util._
import com.typesafe.config.{Config, ConfigFactory}
import io.chronos.resolver.{IvyConfiguration, IvyResolve, Resolver}
import io.chronos.worker.{JobExecutor, Worker}
import scopt.OptionParser

/**
 * Created by domingueza on 09/07/15.
 */
object Boot extends App {

  val parser = new OptionParser[Options]("worker") {
    head("worker", "0.1.0")
    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = b)
    } text "Bind to this external host and port. Useful when using inside Docker containers"
    opt[Int]('p', "port") valueName "<port>" action { (p, options) =>
      options.copy(port = p)
    } text "Worker node port"
    opt[Seq[String]]("master") required() valueName "<host:port>,<host:port>" action { (nodes, options) =>
      options.copy(masterNodes = nodes)
    } text "Comma separated list of Chronos cluster master nodes"
  }

  def loadConfig(opts: Options): Config = {
    val defaultConf = ConfigFactory.load("reference.conf")
    ConfigFactory.parseMap(opts.asJavaMap).
      withFallback(ConfigFactory.load()).
      withFallback(defaultConf)
  }

  def start(config: Config): Unit = {
    val system = ActorSystem("ChronosWorkerSystem", config)
    val initialContacts = immutableSeq(config.getStringList("chronos.contact-points")).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val clusterClient  = system.actorOf(ClusterClient.props(clientSettings), "client")

    val ivyConfig  = IvyConfiguration(config.getConfig("chronos"))
    val ivyResolve = new IvyResolve(ivyConfig)
    val resolver   = system.actorOf(Resolver.props(ivyResolve), "resolver")

    val jobExecutorProps = JobExecutor.props(resolver)
    system.actorOf(Worker.props(clusterClient, jobExecutorProps), "worker")
  }

  parser.parse(args, Options()).foreach { opts =>
    start(loadConfig(opts))
  }

}
