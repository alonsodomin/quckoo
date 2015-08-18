package io.chronos.scheduler.boot

import java.time.Clock

import akka.actor._
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyModuleResolver}
import io.chronos.scheduler.queue.TaskQueue
import io.chronos.scheduler.{Registry, Scheduler}

/**
 * Created by domingueza on 09/07/15.
 */
object Boot {

  val DefaultPort = 2551

  def main(args: Array[String]): Unit = {
    val port = if (args.length > 0) args(0).toInt else DefaultPort

    val defaultConf = ConfigFactory.load("reference.conf")
    val conf = ConfigFactory.parseString("akka.cluster.roles=[scheduler]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load()).
      withFallback(defaultConf)

    val system = ActorSystem("ChronosClusterSystem", conf)
    implicit val clock = Clock.systemUTC()

    val ivyConfig = IvyConfiguration(conf)
    val moduleResolver = new IvyModuleResolver(ivyConfig)

    val registryProps = Registry.props(moduleResolver)
    val queueProps    = TaskQueue.props()
    system.actorOf(Scheduler.props(registryProps, queueProps), "scheduler")
  }

}
