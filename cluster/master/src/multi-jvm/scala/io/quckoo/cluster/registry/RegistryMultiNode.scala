package io.quckoo.cluster.registry

import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestActors, TestProbe}

import io.quckoo.fault._
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Artifact
import io.quckoo.resolver.Resolver
import io.quckoo.{JobSpec, JobPackage}
import io.quckoo.cluster.journal.QuckooTestJournal

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

  final val TestArtifactId = ArtifactId("io.quckoo", "example-jobs_2.11", "0.1.0")
  final val TestJobPackage = JobPackage.jar(
    artifactId = TestArtifactId,
    jobClass = "io.quckoo.examples.paramteters.PowerOfNJob"
  )
  final val TestJobSpec = JobSpec("Examples", jobPackage = TestJobPackage)

}

abstract class RegistryMultiNode extends MultiNodeSpec(RegistryNodesConfig)
    with ImplicitSender with MultiNodeClusterSpec {

  import RegistryMultiNode._
  import RegistryNodesConfig._

  val journal = new QuckooTestJournal

  override def atStartup(): Unit = {
    system.eventStream.subscribe(self, classOf[Registry.Signal])
  }

  "A Registry cluster" should {

    "distribute jobs specs across shards" in {
      awaitClusterUp(registry, proxy)

      Persistence(system)

      runOn(proxy) {
        enterBarrier("registry-ready")

        val registryRef = system.actorSelection(node(registry) / "user" / "registry")

        val invalidJobId = JobId("foo")
        registryRef ! GetJob(invalidJobId)

        val notFoundMsg = expectMsgType[JobNotFound](7 seconds)
        notFoundMsg.jobId shouldBe invalidJobId

        enterBarrier("fetch-invalid-job")

        registryRef ! RegisterJob(TestJobSpec)

        enterBarrier("registering-job")

        val acceptedMsg = expectMsgType[JobAccepted]
        acceptedMsg.job shouldBe TestJobSpec

        registryRef ! GetJob(acceptedMsg.jobId)
        expectMsg(TestJobSpec)

        enterBarrier("job-fetched")

      }

      runOn(registry) {
        val resolverProbe = TestProbe("resolver1")
        val settings = RegistrySettings(TestActors.forwardActorProps(resolverProbe.ref))

        system.actorOf(Registry.props(settings, journal), "registry")
        expectMsg(Registry.Ready)
        enterBarrier("registry-ready")

        enterBarrier("fetch-invalid-job")

        val validateMsg = resolverProbe.expectMsgType[Resolver.Validate]
        validateMsg.artifactId shouldBe TestJobPackage.artifactId

        val artifact = Artifact(validateMsg.artifactId, Seq())
        resolverProbe.reply(Resolver.ArtifactResolved(artifact))

        enterBarrier("registering-job")

        enterBarrier("job-fetched")
      }

      enterBarrier("finished")
    }
  }

}
