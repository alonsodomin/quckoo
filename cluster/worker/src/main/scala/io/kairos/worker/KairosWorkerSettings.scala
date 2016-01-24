package io.kairos.worker

import akka.actor.ActorSystem
import io.kairos.resolver.ivy.IvyConfiguration

/**
  * Created by alonsodomin on 23/01/2016.
  */
object KairosWorkerSettings {

  final val KairosContactPoints = "contact-points"

  def apply(system: ActorSystem): KairosWorkerSettings = {
    val config = system.settings.config


    val kairosConf = config.getConfig("kairos")
    KairosWorkerSettings(IvyConfiguration(kairosConf))
  }

}

case class KairosWorkerSettings private (ivyConfiguration: IvyConfiguration)
