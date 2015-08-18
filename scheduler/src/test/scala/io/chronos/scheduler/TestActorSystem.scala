package io.chronos.scheduler

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
 * Created by aalonsodominguez on 03/08/15.
 */
object TestActorSystem {

  def apply(name: String): ActorSystem = {
    val config = ConfigFactory.load()
    ActorSystem(name, config)
  }

}
