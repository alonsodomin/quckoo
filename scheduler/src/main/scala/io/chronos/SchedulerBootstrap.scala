package io.chronos

import java.time.Clock

import akka.actor._
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyJobModuleResolver}
import io.chronos.scheduler._
import org.apache.ignite.Ignition

/**
 * Created by domingueza on 09/07/15.
 */
object SchedulerBootstrap {

  val DefaultPort = 2551

  def main(args: Array[String]): Unit = {
    val port = if (args.length > 0) args(0).toInt else DefaultPort

    val conf = ConfigFactory.parseString("akka.cluster.roles=[scheduler]")
      .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
      .withFallback(ConfigFactory.load())

    val system = ActorSystem("ChronosClusterSystem", conf)
    implicit val clock = Clock.systemUTC()

    val ignite = Ignition.start()
    val ivyConfig = IvyConfiguration(conf)
    val moduleResolver = new IvyJobModuleResolver(ivyConfig)

    system.actorOf(Props[ClusterMonitor], "monitor")
    system.actorOf(Props[ExecutionMonitor], "executions")

    system.actorOf(RegistryActor.props(ignite, moduleResolver), "registry")
    system.actorOf(SchedulerActor.props(ignite), "scheduler")

    system.actorOf(Props[WorkResultConsumer], "consumer")
  }

}
