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

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit._

import io.quckoo._
import io.quckoo.api.TopicTag
import io.quckoo.protocol.registry._
import io.quckoo.testkit.QuckooActorClusterSuite

import org.scalatest._

import scala.concurrent.duration._

/**
  * Created by domingueza on 21/08/15.
  */
object PersistentJobSpec {

  final val BarArtifactId = ArtifactId("com.example", "bar", "test")
  final val BarJobSpec =
    JobSpec("bar", Some("bar desc"), JobPackage.jar(BarArtifactId, "com.example.bar.Job"))
  final val BarJobId = JobId(BarJobSpec)

}

class PersistentJobSpec
    extends QuckooActorClusterSuite("PersistentJobSpec") with ImplicitSender
    with BeforeAndAfterEach {

  import PersistentJobSpec._

  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_)   => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  val eventListener = TestProbe("persistentJobListener")

  override def beforeEach(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(TopicTag.Registry.name, eventListener.ref)

  override def afterEach(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(TopicTag.Registry.name, eventListener.ref)
    if (eventListener.msgAvailable) {
      fail("There are additional messages in the listener queue.")
    }
  }

  "A persistent job" should {
    val job = TestActorRef(PersistentJob.props.withDispatcher("akka.actor.default-dispatcher"))

    "return job accepted when receiving a create command" in {
      job ! PersistentJob.CreateJob(BarJobId, BarJobSpec)

      val response = eventListener.expectMsgType[JobAccepted]
      response.job shouldBe BarJobSpec
    }

    "return the registered job spec with its status when asked for it" in {
      job ! GetJob(BarJobId)
      expectMsg(BarJobSpec)
    }

    "disable a job that has been previously registered and populate the event to the event stream" in {
      job ! DisableJob(BarJobId)

      eventListener.expectMsgType[JobDisabled].jobId shouldBe BarJobId
      expectMsgType[JobDisabled].jobId shouldBe BarJobId
    }

    "do nothing when trying to disable it again" in {
      job ! DisableJob(BarJobId)

      eventListener.expectNoMsg(500 millis)
      expectMsgType[JobDisabled].jobId shouldBe BarJobId
    }

    "return the registered job spec with disabled status" in {
      job ! GetJob(BarJobId)

      expectMsg(BarJobSpec.copy(disabled = true))
    }

    "enable a job that has been previously disabled and publish the event" in {
      job ! EnableJob(BarJobId)

      eventListener.expectMsgType[JobEnabled].jobId shouldBe BarJobId
      expectMsgType[JobEnabled].jobId shouldBe BarJobId
    }

    "do nothing when trying to enable it again" in {
      job ! EnableJob(BarJobId)

      eventListener.expectNoMsg(500 millis)
      expectMsgType[JobEnabled].jobId shouldBe BarJobId
    }

    "double check that the job is finally enabled" in {
      job ! GetJob(BarJobId)

      expectMsg(BarJobSpec)
    }

  }

}
