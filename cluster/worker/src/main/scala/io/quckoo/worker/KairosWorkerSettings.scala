package io.quckoo.worker

import akka.actor.ActorSystem
import io.quckoo.resolver.ivy.IvyConfiguration

/**
  * Created by alonsodomin on 23/01/2016.
  */
object KairosWorkerSettings {

  final val DefaultTcpInterface = "127.0.0.1"
  final val DefaultTcpPort = 5001

  final val KairosContactPoints = "contact-points"

  def apply(system: ActorSystem): KairosWorkerSettings = {
    val config = system.settings.config


    val kairosConf = config.getConfig("kairos")
    KairosWorkerSettings(IvyConfiguration(kairosConf))
  }

}

case class KairosWorkerSettings private (ivyConfiguration: IvyConfiguration)
