package io.kairos.cluster

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import io.kairos.id.ModuleId
import io.kairos.multijvm.MultiNodeClusterSpec
import io.kairos.protocol.{Connect, Connected}
import io.kairos.test.ImplicitClock

/**
 * Created by domingueza on 26/08/15.
 */
object KairosNodesConfig extends MultiNodeConfig {
  val scheduler = role("scheduler")
  val registry  = role("registry")

  commonConfig(debugConfig(on = false))

  nodeConfig(scheduler)(ConfigFactory.parseString("akka.cluster.roles=[scheduler]").
    withFallback(MultiNodeClusterSpec.clusterConfig))
  nodeConfig(registry)(ConfigFactory.parseString("akka.cluster.roles=[registry]").
    withFallback(MultiNodeClusterSpec.clusterConfig))
}

class KairosMultiNodeClusterSpecMultiJvmNode1 extends KairosMultiNodeCluster
class KairosMultiNodeClusterSpecMultiJvmNode2 extends KairosMultiNodeCluster

object KairosMultiNodeCluster {

  val TestModuleId = ModuleId("io.kairos", "example-jobs_2.11", "0.1.0-SNAPSHOT")

}

abstract class KairosMultiNodeCluster extends MultiNodeSpec(KairosNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitClock {

  import KairosNodesConfig._

  "A Chronos cluster" must {
    val settings = KairosClusterSettings(system)

    "send connect commands from one node to the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        system.actorOf(KairosClusterSupervisor.props(settings), "chronos")
        enterBarrier("deployed")

        val schedulerGuardian = system.actorSelection(node(scheduler) / "user" / "chronos")
        schedulerGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")
      }

      runOn(scheduler) {
        system.actorOf(KairosClusterSupervisor.props(settings), "chronos")
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
