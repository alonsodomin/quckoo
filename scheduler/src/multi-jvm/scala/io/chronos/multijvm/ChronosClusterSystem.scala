package io.chronos.multijvm

import akka.cluster.sharding.ClusterShardingSettings
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestActors}
import com.typesafe.config.ConfigFactory
import io.chronos.cluster.Chronos
import io.chronos.registry.Registry
import io.chronos.scheduler.TaskQueue
import io.chronos.test.ImplicitClock

/**
 * Created by domingueza on 26/08/15.
 */
object ChronosNodesConfig extends MultiNodeConfig {
  val scheduler = role("scheduler")
  val registry  = role("registry")

  commonConfig(ConfigFactory.parseString(
    """
      |akka {
      |  actor.provider=akka.cluster.ClusterActorRefProvider
      |  remote.netty.tcp.hostname=127.0.0.1
      |}
      |""".stripMargin
  ))

  nodeConfig(scheduler)(ConfigFactory.parseString("akka.remote.netty.tcp.port=2551"))
  nodeConfig(registry)(ConfigFactory.parseString("akka.remote.netty.tcp.port=2552"))
}

class ChronosClusterSystemSpecMultiJvmNode1 extends ChronosClusterSystem
class ChronosClusterSystemSpecMultiJvmNode2 extends ChronosClusterSystem

object ChronosClusterSystem {



}

class ChronosClusterSystem extends MultiNodeSpec(ChronosNodesConfig) with ImplicitSender
  with ScalaTestMultiNodeSpec with ImplicitClock {

  import ChronosNodesConfig._

  override def initialParticipants: Int = roles.size

  "A Chronos cluster" must {
    val shardSettings = ClusterShardingSettings(system)

    "wait for all the nodes to be ready" in {
      enterBarrier("startup")
    }

    "registry jobs in one node and fetch them from the other one" in {
      runOn(scheduler) {
        system.actorOf(Chronos.props(shardSettings, TestActors.echoActorProps, TaskQueue.props())(Registry.props))
        enterBarrier("deployed")
      }

      runOn(registry) {
        system.actorOf(Chronos.props(shardSettings, TestActors.echoActorProps, TaskQueue.props())(Registry.props))
        enterBarrier("deployed")
      }
    }

  }

}
