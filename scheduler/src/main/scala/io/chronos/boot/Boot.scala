package io.chronos.boot

import java.time.Clock

import akka.actor._
import com.typesafe.config.{Config, ConfigFactory}
import io.chronos.cluster.{ChronosCluster, ChronosClusterSettings}
import scopt.OptionParser

/**
 * Created by domingueza on 09/07/15.
 */
object Boot extends App {

  val parser = new OptionParser[Options]("scheduler") {
    head("scheduler", "0.1.0")
    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = b)
    } text "Bind to this external host and port. Useful when using inside Docker containers"
    opt[Unit]("seed") action { (_, options) =>
      options.copy(seed = true)
    } text "Flag that indicates that this node will be a seed node. Defaults to true if the list of seed nodes is empty."
    opt[Seq[String]]("nodes") valueName "<host:port>,<host:port>" action { (nodes, options) =>
      options.copy(seedNodes = nodes)
    } text "Comma separated list of Chronos cluster seed nodes"
    opt[Seq[String]]("cs") valueName "<host:port>,<host:port>" action { (seedNodes, options) =>
      options.copy(cassandraSeedNodes = seedNodes)
    } text "Comma separated list of Cassandra seed nodes (same for Journal and Snapshots)"
    help("help") text "prints this usage text"
  }

  def loadConfig(opts: Options): Config = {
    val defaultConf = ConfigFactory.load("reference.conf")
    ConfigFactory.parseMap(opts.asJavaMap).
      withFallback(ConfigFactory.load()).
      withFallback(defaultConf)
  }

  def start(config: Config): Unit = {
    val system = ActorSystem("ChronosClusterSystem", config)
    implicit val clock = Clock.systemUTC()

    val settings = ChronosClusterSettings(system)
    val chronosProps  = ChronosCluster.props(settings)
    system.actorOf(chronosProps, "chronos")
  }

  parser.parse(args, Options()).foreach { opts =>
    start(loadConfig(opts))
  }

}
