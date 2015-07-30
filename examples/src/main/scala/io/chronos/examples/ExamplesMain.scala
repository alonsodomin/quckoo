package io.chronos.examples

import akka.actor.{ActorSystem, AddressFromURIString, Props, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import io.chronos.examples.parameters.PowerOfNActor

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExamplesMain extends App {

  val chronosConf = ConfigFactory.load()

  val system = ActorSystem("ChronosExamplesSystem", chronosConf)

  val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
    case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  val client = system.actorOf(ClusterClient.props(initialContacts), "client")

  val frontend = system.actorOf(FacadeActor.props(client), "frontend")
  system.actorOf(Props(classOf[PowerOfNActor], frontend), "producer")

}
