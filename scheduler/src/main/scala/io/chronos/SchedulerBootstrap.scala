package io.chronos

import java.time.Clock

import akka.actor._
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.{IvyConfiguration, IvyModuleResolver}
import io.chronos.scheduler._
import io.chronos.scheduler.cache._
import io.chronos.scheduler.concurrent.NonBlockingSync

/**
 * Created by domingueza on 09/07/15.
 */
object SchedulerBootstrap {

  val DefaultPort = 2551

  def main(args: Array[String]): Unit = {
    val port = if (args.length > 0) args(0).toInt else DefaultPort

    val conf = ConfigFactory.parseString("akka.cluster.roles=[scheduler]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ChronosClusterSystem", conf)
    implicit val clock = Clock.systemUTC()

    val hazelcastInstance = Hazelcast.newHazelcastInstance()
    val jobCache = new JobCache(hazelcastInstance)
    val scheduleCache = new ScheduleCache(hazelcastInstance)
    val executionCache = new ExecutionCache(hazelcastInstance)
    val executionQueue = new ExecutionQueue(hazelcastInstance)
    val clusterSync = new NonBlockingSync(hazelcastInstance, "sweep")

    val ivyConfig = IvyConfiguration(conf)
    val moduleResolver = new IvyModuleResolver(ivyConfig)

    system.actorOf(Props[ClusterMonitor], "monitor")
    system.actorOf(Props[ExecutionMonitor], "executions")

    system.actorOf(RegistryActor.props(jobCache, moduleResolver), "registry")
    val executionPlanner = system.actorOf(ExecutionPlanActor.props(scheduleCache, executionCache), "plan")
    system.actorOf(SchedulerActor.props(executionPlanner, jobCache, scheduleCache, executionCache, executionQueue, clusterSync), "scheduler")
  }

}
