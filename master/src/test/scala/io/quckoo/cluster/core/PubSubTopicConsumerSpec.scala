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

package io.quckoo.cluster.core

import java.util.UUID

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit.{TestActorRef, TestProbe}

import io.quckoo.NodeId
import io.quckoo.api.TopicTag
import io.quckoo.protocol.cluster.MasterRemoved
import io.quckoo.testkit.QuckooActorClusterSuite

import scala.concurrent.duration._

/**
  * Created by domingueza on 28/02/2017.
  */
class PubSubTopicConsumerSpec extends QuckooActorClusterSuite("PubSubTopicConsumerSpec") {

  val mediator = DistributedPubSub(system).mediator

  val topicTag = TopicTag.Master
  val consumer = TestActorRef[PubSubTopicConsumer](
    PubSubTopicConsumer.props(topicTag).withDispatcher("akka.actor.default-dispatcher")
  )

  "PubSubTopicConsumer" should {
    "emit events to its sender" in {
      val receiverProbe = TestProbe("receiver")

      val expectedMsg = MasterRemoved(NodeId(UUID.randomUUID()))

      // Receiver instructs the reader to start, linking them together
      receiverProbe.send(consumer, TopicConsumer.Consume)

      mediator ! DistributedPubSubMediator.Publish(topicTag.name, expectedMsg)
      receiverProbe.expectMsg(5 seconds, expectedMsg)
    }
  }

}
