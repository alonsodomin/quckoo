package io.quckoo.worker

import akka.actor.ActorSystem
import io.quckoo.resolver.ivy.IvyConfiguration

/**
  * Created by alonsodomin on 23/01/2016.
  */
object QuckooWorkerSettings {

  final val DefaultTcpInterface = "127.0.0.1"
  final val DefaultTcpPort = 5001

  final val QuckooContactPoints = "contact-points"

  def apply(system: ActorSystem): QuckooWorkerSettings = {
    val sysConfig = system.settings.config

    val quckooConf = sysConfig.getConfig("quckoo")
    QuckooWorkerSettings(IvyConfiguration(quckooConf))
  }

}

case class QuckooWorkerSettings private(ivyConfiguration: IvyConfiguration)
