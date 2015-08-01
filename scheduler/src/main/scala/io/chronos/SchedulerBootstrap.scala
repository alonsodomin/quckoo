package io.chronos

import java.time.Clock

import akka.actor._
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyJobModuleResolver}
import io.chronos.scheduler._
import io.chronos.scheduler.store.HazelcastStore

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

    val hazelcastInstance = Hazelcast.newHazelcastInstance()
    val store = new HazelcastStore(hazelcastInstance)

    val ivyConfig = IvyConfiguration(conf)
    val moduleResolver = new IvyJobModuleResolver(ivyConfig)

    system.actorOf(Props[ClusterMonitor], "monitor")
    system.actorOf(Props[ExecutionMonitor], "executions")

    val registry = system.actorOf(RegistryActor.props(store, moduleResolver), "registry")
    system.actorOf(SchedulerActor.props(hazelcastInstance, registry), "scheduler")
  }

}
