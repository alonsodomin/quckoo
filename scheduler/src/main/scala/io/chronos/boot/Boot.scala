package io.chronos.boot

import java.time.Clock

import akka.actor._
import akka.cluster.sharding.ClusterShardingSettings
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import io.chronos.cluster.Chronos
import io.chronos.registry.Registry
import io.chronos.resolver.{IvyConfiguration, IvyResolve, Resolver}
import io.chronos.scheduler.TaskQueue

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
    val ivyResolver = new IvyResolve(ivyConfig)

    val shardSettings = ClusterShardingSettings(system)

    val resolverProps = Resolver.props(ivyResolver).
      withRouter(RoundRobinPool(3))

    val queueProps    = TaskQueue.props()
    val chronosProps  = Chronos.props(shardSettings, resolverProps, queueProps) { Registry.props }
    system.actorOf(chronosProps, "chronos")
  }

}
