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

import akka.testkit.{ImplicitSender, TestActors, TestActorRef, TestProbe}

import io.quckoo.{JobSpec, JobPackage, JarJobPackage, ShellScriptPackage}
import io.quckoo.cluster.journal.QuckooTestJournal
import io.quckoo.protocol.registry._
import io.quckoo.testkit.QuckooActorClusterSuite

object RegistrySpec {

  final val TestJobPackage = JobPackage.shell("echo \"hello\"")
  final val TestJobSpec = JobSpec("Foo", jobPackage = TestJobPackage)

}

class RegistrySpec extends QuckooActorClusterSuite("RegistrySpec") with ImplicitSender {
  import RegistrySpec._

  val journal = new QuckooTestJournal

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    system.eventStream.subscribe(self, classOf[Registry.Signal])
  }

  "The registry" should {
    val resolverProbe = TestProbe("resolver1")
    val settings = RegistrySettings(TestActors.forwardActorProps(resolverProbe.ref))
    val registry = TestActorRef(Registry.props(settings, journal))

    "complete warm up process" in {
      expectMsg(Registry.Ready)
    }

    "register shell script jobs" in {
      registry ! RegisterJob(TestJobSpec)

      expectMsgType[JobAccepted]
    }
  }

}
