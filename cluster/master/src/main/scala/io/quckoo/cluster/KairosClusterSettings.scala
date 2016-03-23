package io.quckoo.cluster

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import io.quckoo.resolver.ivy.IvyConfiguration

import scala.concurrent.duration._

/**
 * Created by domingueza on 28/08/15.
 */
object KairosClusterSettings {

  final val DefaultHttpInterface = "0.0.0.0"
  final val DefaultHttpPort = 8095

  final val DefaultTcpInterface = "127.0.0.1"
  final val DefaultTcpPort = 2551

  def apply(system: ActorSystem): KairosClusterSettings = {
    val config = system.settings.config.getConfig(BaseConfigNamespace)
    KairosClusterSettings(
      IvyConfiguration(config),
      config.getDuration("task-queue.max-work-timeout", TimeUnit.MILLISECONDS) millis,
      config.getString("http.bind-interface"),
      config.getInt("http.bind-port")
    )
  }

}

case class KairosClusterSettings private (
    ivyConfiguration: IvyConfiguration,
    queueMaxWorkTimeout: FiniteDuration,
    httpInterface: String = KairosClusterSettings.DefaultHttpInterface,
    httpPort: Int = KairosClusterSettings.DefaultHttpPort
)
