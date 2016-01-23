package io.kairos.cluster.boot

import akka.actor._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import io.kairos.cluster.{KairosCluster, KairosClusterSettings}
import io.kairos.time.JDK8TimeSource
import scopt.OptionParser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
    } text "Comma separated list of Kairos cluster seed nodes"
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
    implicit val system = ActorSystem("KairosClusterSystem", config)
    sys.addShutdownHook { system.terminate() }

    implicit val materializer = ActorMaterializer()

    implicit val timeSource = JDK8TimeSource.default
    val settings = KairosClusterSettings(system)
    val cluster = new KairosCluster(settings)

    implicit val timeout = Timeout(5 seconds)
    val startUp: Future[Unit] = cluster.start recover {
      case ex: Exception =>
        ex.printStackTrace()
        system.terminate()
    }

    Await.ready(startUp, 10 seconds)
  }

  parser.parse(args, Options()).foreach { opts =>
    start(loadConfig(opts))
  }

}
