package io.chronos.examples

import akka.actor.{ActorSystem, AddressFromURIString, Props, RootActorPath}
import akka.cluster.client.ClusterClientSettings
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import io.chronos.client.ChronosClient
import io.chronos.examples.parameters.PowerOfNActor

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExamplesMain extends App {

  val chronosConf = ConfigFactory.load()

  val system = ActorSystem("ChronosExamplesSystem", chronosConf)

  val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
    case AddressFromURIString(addr) => RootActorPath(addr) / "user" / "receptionist"
  }.toSet

  val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
  val chronosClient = system.actorOf(ChronosClient.props(clientSettings), "chronosClient")

  system.actorOf(Props(classOf[PowerOfNActor], chronosClient), "powerOfN")

}
