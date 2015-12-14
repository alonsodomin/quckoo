package io.kairos.cluster.registry

import akka.actor.{Props, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestProbe}
import io.kairos.JobSpec
import io.kairos.cluster.core.ForwadingReceptionist
import io.kairos.id.{JobId, ModuleId}
import io.kairos.multijvm.MultiNodeClusterSpec
import io.kairos.protocol.RegistryProtocol.{JobAccepted, RegisterJob}
import io.kairos.protocol.ResolutionFailed
import io.kairos.resolver.{Resolver, JobPackage}
import io.kairos.resolver.Resolver.Validate

/**
 * Created by domingueza on 28/08/15.
 */
object RegistryNodesConfig extends MultiNodeConfig {
  val registry = role("registry")
  val proxy = role("proxy")

  commonConfig(MultiNodeClusterSpec.clusterConfig)
}

class RegistryMultiNodeSpecMultiJvmNode1 extends RegistryMultiNode
class RegistryMultiNodeSpecMultiJvmProxy extends RegistryMultiNode

object RegistryMultiNode {

  final val TestModuleId = ModuleId("io.kairos", "example-jobs_2.11", "0.1.0-SNAPSHOT")
  final val TestJobSpec = JobSpec("Examples", moduleId = TestModuleId, jobClass = "io.kairos.examples.paramteters.PowerOfNJob")
  final val TestJobPackage = JobPackage(TestModuleId, Seq())

}

abstract class RegistryMultiNode extends MultiNodeSpec(RegistryNodesConfig) with ImplicitSender with MultiNodeClusterSpec {

  import RegistryMultiNode._
  import RegistryNodesConfig._

  "A Registry cluster" should {

    "distribute jobs specs across shards" in {
      awaitClusterUp(registry, proxy)

      Persistence(system)

      runOn(proxy) {
        enterBarrier("shard-ready")

        val registryRef = system.actorSelection(node(registry) / "user" / "registry")
        registryRef ! RegisterJob(TestJobSpec)

        enterBarrier("registering-job")

        expectMsgType[JobAccepted].job should be(TestJobSpec)
      }

      runOn(registry) {
        val resolverProbe = TestProbe()
        val ref = ClusterSharding(system).start(
          typeName        = Registry.shardName,
          entityProps     = Registry.props(resolverProbe.ref),
          settings        = ClusterShardingSettings(system),
          extractEntityId = Registry.idExtractor,
          extractShardId  = Registry.shardResolver
        )
        system.actorOf(Props(classOf[ForwadingReceptionist], ref), "registry")
        enterBarrier("shard-ready")

        enterBarrier("registering-job")
        resolverProbe.expectMsgType[Validate].moduleId should be(TestModuleId)
        resolverProbe.reply(TestJobPackage)
      }

      enterBarrier("finished")
    }
  }

}
