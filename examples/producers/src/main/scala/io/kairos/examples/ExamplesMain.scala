package io.kairos.examples

import akka.actor.{ActorSystem, AddressFromURIString, Props, RootActorPath}
import akka.cluster.client.ClusterClientSettings
import akka.japi.Util._
import com.typesafe.config.{Config, ConfigFactory}
import io.kairos.examples.parameters.PowerOfNActor
import io.kairos.protocol.Connect
import scopt.OptionParser

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExamplesMain extends App {

  val parser = new OptionParser[CliOptions]("example-producers") {
    head("example-producers", "0.1.0")
    opt[Seq[String]]('c', "cluster") required() valueName "<host>:<port>" action { (c, options) =>
      options.copy(clusterNodes = c)
    } text "Comma separated list of Chronos cluster nodes to connect to"
  }

  def loadConfig(opts: CliOptions): Config =
    ConfigFactory.parseMap(opts.asJavaMap).
      withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    val system = ActorSystem("ChronosExamplesSystem", config)

    val initialContacts = immutableSeq(config.getStringList("chronos.contact-points")).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val chronosClient = system.actorOf(KairosClient.props(clientSettings), "chronosClient")
    chronosClient ! Connect

    system.actorOf(Props(classOf[PowerOfNActor], chronosClient), "powerOfN")
  }

  parser.parse(args, CliOptions()).foreach { opts =>
    start(loadConfig(opts))
  }

}
