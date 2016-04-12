package io.quckoo.cluster.registry

import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestProbe}
import io.quckoo.cluster.QuckooClusterSettings
import io.quckoo.fault.Fault
import io.quckoo.id.ArtifactId
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.registry._
import io.quckoo.resolver.{Artifact, Resolve}
import io.quckoo.JobSpec
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{ExecutionContext, Future}
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

abstract class RegistryMultiNode extends MultiNodeSpec(RegistryNodesConfig)
    with ImplicitSender with MultiNodeClusterSpec with MockFactory {

  import RegistryMultiNode._
  import RegistryNodesConfig._

  import Scalaz._

  implicit val materializer = ActorMaterializer()
  val mockResolve = mock[Resolve]

  "A Registry cluster" should {
    val settings = QuckooClusterSettings(system)

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
        /*(mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
          expects(TestArtifactId, false, *).
          returning(Future.successful(TestArtifact.successNel[Fault]))*/

        val ref = ClusterSharding(system).start(
          typeName        = RegistryShard.ShardName,
          entityProps     = RegistryShard.props(resolverProbe.ref),
          settings        = ClusterShardingSettings(system),
          extractEntityId = RegistryShard.idExtractor,
          extractShardId  = RegistryShard.shardResolver
        )
        system.actorOf(Registry.props(settings), "registry")
        enterBarrier("shard-ready")

        enterBarrier("registering-job")
      }

      enterBarrier("finished")
    }
  }

}
