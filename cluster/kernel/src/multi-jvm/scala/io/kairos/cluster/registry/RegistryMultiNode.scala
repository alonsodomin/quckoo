package io.kairos.cluster.registry

import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestProbe}
import io.kairos.JobSpec
import io.kairos.cluster.core.RegistryReceptionist
import io.kairos.id.ArtifactId
import io.kairos.multijvm.MultiNodeClusterSpec
import io.kairos.protocol.Fault
import io.kairos.protocol.RegistryProtocol.{JobAccepted, RegisterJob}
import io.kairos.resolver.Artifact
import io.kairos.resolver.Resolver.Validate

import scalaz._

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

  final val TestArtifactId = ArtifactId("io.kairos", "example-jobs_2.11", "0.1.0-SNAPSHOT")
  final val TestJobSpec = JobSpec("Examples", artifactId = TestArtifactId, jobClass = "io.kairos.examples.paramteters.PowerOfNJob")
  final val TestArtifact = Artifact(TestArtifactId, Seq())

}

abstract class RegistryMultiNode extends MultiNodeSpec(RegistryNodesConfig) with ImplicitSender with MultiNodeClusterSpec {

  import RegistryMultiNode._
  import RegistryNodesConfig._

  import Scalaz._

  implicit val materializer = ActorMaterializer()

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
        system.actorOf(RegistryReceptionist.props(ref), "registry")
        enterBarrier("shard-ready")

        enterBarrier("registering-job")
        resolverProbe.expectMsgType[Validate].artifactId should be(TestArtifactId)
        resolverProbe.reply(TestArtifact.successNel[Fault])
      }

      enterBarrier("finished")
    }
  }

}
