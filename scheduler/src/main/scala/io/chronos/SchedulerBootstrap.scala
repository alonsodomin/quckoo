package io.chronos

import java.time.Clock

import akka.actor._
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import io.chronos.scheduler.{Scheduler, _}

/**
 * Created by domingueza on 09/07/15.
 */
object SchedulerBootstrap extends App {

  val DefaultPort = 2551

  val hazelcastInstance = Hazelcast.newHazelcastInstance()
  val jobRegistry = new HazelcastJobRegistry(hazelcastInstance)

  val port = if (args.length > 0) args(0).toInt else DefaultPort

  startScheduler(port, "scheduler")

  def startScheduler(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]")
      .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)
    val clock = Clock.systemUTC()

    system.actorOf(Props[ClusterMonitor], "monitor")
    system.actorOf(Props[ExecutionMonitor], "executions")

    system.actorOf(Scheduler.props(clock, jobRegistry), "scheduler")
    system.actorOf(Repository.props(jobRegistry), "repository")

    system.actorOf(Props[WorkResultConsumer], "consumer")
  }

}
