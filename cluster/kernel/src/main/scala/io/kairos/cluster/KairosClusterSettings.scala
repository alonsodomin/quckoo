package io.kairos.cluster

import akka.actor.ActorSystem
import io.kairos.resolver.IvyConfiguration

import scala.concurrent.duration._

/**
 * Created by domingueza on 28/08/15.
 */
object KairosClusterSettings {

  def apply(system: ActorSystem): KairosClusterSettings = {
    val config = system.settings.config.getConfig("kairos")
    KairosClusterSettings(
      IvyConfiguration(config),
      config.getDuration("task-queue.max-work-timeout").toMillis millis,
      config.getString("http.bind-interface"),
      config.getInt("http.bind-port")
    )
  }

}

case class KairosClusterSettings private(ivyConfiguration: IvyConfiguration,
                                         queueMaxWorkTimeout: FiniteDuration,
                                         httpInterface: String = "0.0.0.0",
                                         httpPort: Int = 8080)