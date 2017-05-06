/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import akka.persistence.Persistence
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestProbe}
import io.quckoo._
import io.quckoo.reflect.Artifact
import io.quckoo.cluster.journal.QuckooTestJournal
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Resolver
import io.quckoo.testkit.QuckooActorClusterSuite

import scala.concurrent.duration._

object RegistrySpec {

  final val TestShellJobPackage = JobPackage.shell("echo \"hello\"")
  final val TestShellJobSpec = JobSpec("Foo", jobPackage = TestShellJobPackage)

  final val TestJarJobPackage = JobPackage.jar(ArtifactId(
    "io.quckoo", "quckoo-example-jobs_2.11", "0.1.0"
  ), "io.quckoo.examples.HelloWorldJob")
  final val TestJarJobSpec = JobSpec("Bar", jobPackage = TestJarJobPackage)

}

class RegistrySpec extends QuckooActorClusterSuite("RegistrySpec") with ImplicitSender {
  import RegistrySpec._

  val journal = new QuckooTestJournal
  Persistence(system)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    system.eventStream.subscribe(self, classOf[Registry.Signal])
  }

  override protected def afterAll(): Unit = {
    system.eventStream.unsubscribe(self)
    super.afterAll()
  }

  "The registry" should {
    val resolverProbe = TestProbe("resolver1")
    val settings = RegistrySettings(TestActors.forwardActorProps(resolverProbe.ref))
    val registry = TestActorRef(Registry.props(settings, journal).withDispatcher("akka.actor.default-dispatcher"))

    "complete warm up process" in {
      expectMsg(5 seconds, Registry.Ready)
    }

    "return JobNotFound for non existent jobs" in {
      val jobId = JobId("bar")

      registry ! GetJob(jobId)

      expectMsg(JobNotFound(jobId))
    }

    "register shell script jobs" in {
      registry ! RegisterJob(TestShellJobSpec)

      resolverProbe.expectNoMsg(500 millis)

      val acceptedMsg = expectMsgType[JobAccepted]
      acceptedMsg.jobId shouldBe JobId(TestShellJobSpec)
    }

    "return the job spec for the shell script" in {
      registry ! GetJob(JobId(TestShellJobSpec))

      expectMsg(TestShellJobSpec)
    }

    "register jar jobs" in {
      registry ! RegisterJob(TestJarJobSpec)

      resolverProbe.expectMsg(Resolver.Validate(TestJarJobPackage.artifactId))
      resolverProbe.reply(Resolver.ArtifactResolved(Artifact(TestJarJobPackage.artifactId, List.empty)))

      val acceptedMsg = expectMsgType[JobAccepted]
      acceptedMsg.jobId shouldBe JobId(TestJarJobSpec)
    }

    "return the job spec for the jar" in {
      registry ! GetJob(JobId(TestJarJobSpec))

      expectMsg(TestJarJobSpec)
    }
  }

}
