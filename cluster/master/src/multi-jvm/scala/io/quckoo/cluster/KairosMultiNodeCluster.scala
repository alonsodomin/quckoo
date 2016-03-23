package io.quckoo.cluster

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import io.quckoo.cluster.core.KairosCluster
import io.quckoo.id.ArtifactId
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.ClientProtocol
import io.quckoo.test.ImplicitTimeSource

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

  val TestArtifactId = ArtifactId("io.kairos", "example-jobs_2.11", "0.1.0-SNAPSHOT")

}

abstract class KairosMultiNodeCluster extends MultiNodeSpec(KairosNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitTimeSource {

  import ClientProtocol._
  import KairosNodesConfig._

  implicit val materializer = ActorMaterializer()

  "A Chronos cluster" must {
    val settings = KairosClusterSettings(system)

    "send connect commands from one node to the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        system.actorOf(KairosCluster.props(settings), "chronos")
        enterBarrier("deployed")

        val schedulerGuardian = system.actorSelection(node(scheduler) / "user" / "chronos")
        schedulerGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")
      }

      runOn(scheduler) {
        system.actorOf(KairosCluster.props(settings), "chronos")
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
