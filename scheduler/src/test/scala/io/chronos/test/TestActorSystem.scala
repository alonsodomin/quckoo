package io.chronos.test

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory

/**
 * Created by aalonsodominguez on 03/08/15.
 */
object TestActorSystem {

  def apply(name: String): ActorSystem = {
    val config = ConfigFactory.load("test-application")
    val system = ActorSystem(name, config)

    val address = Cluster(system).selfAddress
    Cluster(system).join(address)

    system
  }

}
