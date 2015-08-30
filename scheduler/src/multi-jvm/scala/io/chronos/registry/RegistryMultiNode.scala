package io.chronos.registry

import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.remote.testkit.{MultiNodeSpec, MultiNodeConfig}
import akka.testkit.{TestProbe, ImplicitSender}
import io.chronos.id.ModuleId
import io.chronos.multijvm.MultiNodeClusterSpec

/**
 * Created by domingueza on 28/08/15.
 */
object RegistryNodesConfig extends MultiNodeConfig {
  val registry1 = role("registry1")
  val registry2 = role("registry2")

  commonConfig(MultiNodeClusterSpec.clusterConfig)
}

class RegistryMultiNodeSpecMultiJvmNode1 extends RegistryMultiNode
class RegistryMultiNodeSpecMultiJvmNode2 extends RegistryMultiNode

object RegistryMultiNode {
  val TestModuleId = ModuleId("io.chronos", "example-jobs_2.11", "0.1.0-SNAPSHOT")
}

abstract class RegistryMultiNode extends MultiNodeSpec(RegistryNodesConfig) with ImplicitSender with MultiNodeClusterSpec {

  import RegistryNodesConfig._

  "A Registry cluster" should {
    val resolverProbe = TestProbe()

    "distribute jobs across shards" in {
      awaitClusterUp(registry1, registry2)

      runOn(registry1) {
        ClusterSharding(system).start(
          typeName        = Registry.shardName,
          entityProps     = Registry.props(resolverProbe.ref),
          settings        = ClusterShardingSettings(system),
          extractEntityId = Registry.idExtractor,
          extractShardId  = Registry.shardResolver
        )
        enterBarrier("shard-started")
      }

      runOn(registry2) {
        ClusterSharding(system).start(
          typeName        = Registry.shardName,
          entityProps     = Registry.props(resolverProbe.ref),
          settings        = ClusterShardingSettings(system),
          extractEntityId = Registry.idExtractor,
          extractShardId  = Registry.shardResolver
        )
        enterBarrier("shard-started")
      }


    }
  }

}
