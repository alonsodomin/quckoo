/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.registry

import java.nio.file.Files

import akka.actor.Props
import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestActors, TestProbe}
import io.quckoo._
import io.quckoo.reflect.Artifact
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.registry._
import io.quckoo.resolver.{PureResolver, Resolver}
import io.quckoo.cluster.QuckooRoles
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.journal.QuckooTestJournal
import io.quckoo.resolver.config.IvyConfig

import scala.concurrent.duration._

/**
 * Created by domingueza on 28/08/15.
 */
object RegistryNodesConfig extends MultiNodeConfig {
  val registry = role(QuckooRoles.Registry)
  val proxy = role("other")

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

  private final val TestBasePath = Files.createTempDirectory("ivyresolve")

  final val TestIvyConfig = IvyConfig(
    TestBasePath.toFile,
    Files.createTempDirectory(TestBasePath, "resolution-cache").toFile,
    Files.createTempDirectory(TestBasePath, "repository-cache").toFile
  )

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
        val artifact = Artifact(TestJobPackage.artifactId, List())

        val registryProps = Props(new Registry(new PureResolver(artifact), journal))
        system.actorOf(registryProps, "registry")

        expectMsg(Registry.Ready)
        enterBarrier("registry-ready")

        enterBarrier("fetch-invalid-job")

        enterBarrier("registering-job")

        enterBarrier("job-fetched")
      }

      enterBarrier("finished")
    }
  }

}
