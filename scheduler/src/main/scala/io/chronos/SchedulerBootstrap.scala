package io.chronos

import java.time.Clock

import akka.actor._
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyJobModuleResolver}
import io.chronos.scheduler.{Scheduler, _}
import org.codehaus.plexus.classworlds.ClassWorld

/**
 * Created by domingueza on 09/07/15.
 */
object SchedulerBootstrap extends App {

  val DefaultPort = 2551

  val port = if (args.length > 0) args(0).toInt else DefaultPort

  val conf = ConfigFactory.parseString("akka.cluster.roles=[scheduler]")
    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
    .withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterSystem", conf)
  val clock = Clock.systemUTC()

  val hazelcastInstance = Hazelcast.newHazelcastInstance()
  val classWorld = new ClassWorld("chronos.worker", Thread.currentThread().getContextClassLoader)
  val jobRegistry = new HazelcastJobRegistry(hazelcastInstance)

  val ivyConfig = IvyConfiguration(conf)
  val moduleResolver = new IvyJobModuleResolver(ivyConfig)

  system.actorOf(Props[ClusterMonitor], "monitor")
  system.actorOf(Props[ExecutionMonitor], "executions")

  system.actorOf(Scheduler.props(clock, jobRegistry), "scheduler")
  system.actorOf(Registry.props(jobRegistry, classWorld, moduleResolver), "repository")

  system.actorOf(Props[WorkResultConsumer], "consumer")

}
