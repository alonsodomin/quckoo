package io.quckoo.cluster.registry

import java.util.UUID

import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestActors, TestProbe}

import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Artifact
import io.quckoo.resolver.Resolver
import io.quckoo.JobSpec

import scala.concurrent.duration._

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
  final val TestJobSpec = JobSpec(
    "Examples", artifactId = TestArtifactId,
    jobClass = "io.kairos.examples.paramteters.PowerOfNJob"
  )

}

abstract class RegistryMultiNode extends MultiNodeSpec(RegistryNodesConfig)
    with ImplicitSender with MultiNodeClusterSpec {

  import RegistryMultiNode._
  import RegistryNodesConfig._

  "A Registry cluster" should {

    "distribute jobs specs across shards" in {
      awaitClusterUp(registry, proxy)

      Persistence(system)

      runOn(proxy) {
        enterBarrier("registry-ready")

        val registryRef = system.actorSelection(node(registry) / "user" / "registry")

        val invalidJobId = JobId(UUID.randomUUID())
        registryRef ! GetJob(invalidJobId)

        val notFoundMsg = expectMsgType[JobNotFound](7 seconds)
        notFoundMsg.jobId shouldBe invalidJobId

        enterBarrier("fetch-invalid-job")

        registryRef ! RegisterJob(TestJobSpec)

        enterBarrier("registering-job")

        val acceptedMsg = expectMsgType[JobAccepted]
        acceptedMsg.job shouldBe TestJobSpec

        registryRef ! GetJob(acceptedMsg.jobId)
        expectMsg(acceptedMsg.jobId -> TestJobSpec)

        enterBarrier("job-fetched")

      }

      runOn(registry) {
        val resolverProbe = TestProbe("resolver1")
        val settings = RegistrySettings(TestActors.forwardActorProps(resolverProbe.ref))

        system.actorOf(Registry.props(settings), "registry")
        enterBarrier("registry-ready")

        enterBarrier("fetch-invalid-job")

        val validateMsg = resolverProbe.expectMsgType[Resolver.Validate]
        validateMsg.artifactId shouldBe TestJobSpec.artifactId

        val artifact = Artifact(validateMsg.artifactId, Seq())
        resolverProbe.reply(Resolver.ArtifactResolved(artifact))

        enterBarrier("registering-job")

        enterBarrier("job-fetched")
      }

      enterBarrier("finished")
    }
  }

}
