package io.quckoo.worker.boot

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.japi.Util._
import com.typesafe.config.{Config, ConfigFactory}
import io.quckoo.resolver.ivy.{IvyConfiguration, IvyResolve}
import io.quckoo.worker.{JobExecutor, Worker}
import scopt.OptionParser

/**
 * Created by domingueza on 09/07/15.
 */
object Boot extends App {

  val parser = new OptionParser[Options]("quckoo-worker") {
    head("quckoo-worker", "0.1.0")
    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = Some(b))
    } text "Bind to this external host and port. Useful when using inside Docker containers"

    opt[Int]('p', "port") valueName "<port>" action { (p, options) =>
      options.copy(port = p)
    } text "Worker node port"

    opt[Seq[String]]("master") required() valueName "<host:port>,<host:port>" action { (nodes, options) =>
      options.copy(masterNodes = nodes)
    } text "Comma separated list of Quckoo master nodes"
  }

  def loadConfig(opts: Options): Config =
    opts.toConfig.withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    val system = ActorSystem(Options.SystemName, config)
    sys.addShutdownHook { system.terminate() }

    val initialContacts = immutableSeq(config.getStringList(Options.QuckooContactPoints)).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val clusterClient  = system.actorOf(ClusterClient.props(clientSettings), "client")

    val ivyConfig  = IvyConfiguration(config.getConfig("quckoo"))
    val ivyResolve = IvyResolve(ivyConfig)
    val jobExecutorProps = JobExecutor.props(ivyResolve)
    system.actorOf(Worker.props(clusterClient, jobExecutorProps), "worker")
  }

  parser.parse(args, Options()).foreach { opts =>
    start(loadConfig(opts))
  }

}
