package io.chronos.multijvm

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import io.chronos.cluster.ChronosCluster
import io.chronos.id.ModuleId
import io.chronos.protocol.{Connect, Connected}
import io.chronos.scheduler.TaskQueue
import io.chronos.test.ImplicitClock

/**
 * Created by domingueza on 26/08/15.
 */
object ChronosNodesConfig extends MultiNodeConfig {
  val scheduler = role("scheduler")
  val registry  = role("registry")

  commonConfig(debugConfig(on = false))

  nodeConfig(scheduler)(ConfigFactory.parseString("akka.cluster.roles=[scheduler]").
    withFallback(MultiNodeClusterSpec.clusterConfig))
  nodeConfig(registry)(ConfigFactory.parseString("akka.cluster.roles=[registry]").
    withFallback(MultiNodeClusterSpec.clusterConfig))
}

class ChronosMultiNodeClusterSpecMultiJvmNode1 extends ChronosMultiNodeCluster
class ChronosMultiNodeClusterSpecMultiJvmNode2 extends ChronosMultiNodeCluster

object ChronosMultiNodeCluster {

  val TestModuleId = ModuleId("io.chronos", "example-jobs_2.11", "0.1.0-SNAPSHOT")

}

abstract class ChronosMultiNodeCluster extends MultiNodeSpec(ChronosNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitClock {

  import ChronosNodesConfig._

  override def initialParticipants: Int = roles.size

  "A Chronos cluster" must {

    "send connect commands from one node to the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        system.actorOf(ChronosCluster.props(TaskQueue.props()), "chronos")
        enterBarrier("deployed")

        val schedulerGuardian = system.actorSelection(node(scheduler) / "user" / "chronos")
        schedulerGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")
      }

      runOn(scheduler) {
        system.actorOf(ChronosCluster.props(TaskQueue.props()), "chronos")
        enterBarrier("deployed")

        val registryGuardian = system.actorSelection(node(registry) / "user" / "chronos")
        registryGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")
      }

      enterBarrier("finished")
    }

  }

}
