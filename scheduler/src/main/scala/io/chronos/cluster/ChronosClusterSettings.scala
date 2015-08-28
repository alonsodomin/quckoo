package io.chronos.cluster

import akka.actor.ActorSystem
import io.chronos.resolver.IvyConfiguration

import scala.concurrent.duration._

/**
 * Created by domingueza on 28/08/15.
 */
object ChronosClusterSettings {

  def apply(system: ActorSystem): ChronosClusterSettings = {
    val config = system.settings.config.getConfig("chronos")
    ChronosClusterSettings(
      IvyConfiguration(config),
      config.getDuration("task-queue.max-work-timeout").toMillis millis
    )
  }

}

case class ChronosClusterSettings private (ivyConfiguration: IvyConfiguration,
                                           queueMaxWorkTimeout: FiniteDuration)
