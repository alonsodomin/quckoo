package io.quckoo.cluster.boot

import akka.actor._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import io.quckoo.cluster.{Kairos, KairosClusterSettings}
import io.quckoo.time.JDK8TimeSource
import scopt.OptionParser

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Created by domingueza on 09/07/15.
 */
object Boot extends App {

  val parser = new OptionParser[Options]("scheduler") {
    head("scheduler", "0.1.0")
    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = Some(b))
    } text "Bind to this external host and port. Useful when using inside Docker containers"
    opt[Int]('p', "port") valueName "port" action { (p, options) =>
      options.copy(port = p)
    } text "Port to use to listen to connections"
    opt[Int]("httpPort") valueName "port" action { (p, options) =>
      options.copy(httpPort = Some(p))
    } text "HTTP port to use to serve the web UI"
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

  def loadConfig(opts: Options): Config =
    opts.toConfig.withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    implicit val system = ActorSystem("KairosClusterSystem", config)
    sys.addShutdownHook { system.terminate() }

    implicit val materializer = ActorMaterializer()

    implicit val timeSource = JDK8TimeSource.default
    val settings = KairosClusterSettings(system)
    val kairos = new Kairos(settings)

    implicit val timeout = Timeout(5 seconds)
    implicit val dispatcher = system.dispatcher
    val startUp: Future[Unit] = kairos.start recover {
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
