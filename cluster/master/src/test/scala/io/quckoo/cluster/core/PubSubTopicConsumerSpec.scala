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

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit.{TestActorRef, TestProbe}

import io.quckoo.api.TopicTag
import io.quckoo.testkit.QuckooActorClusterSuite

/**
  * Created by domingueza on 28/02/2017.
  */
class PubSubTopicConsumerSpec extends QuckooActorClusterSuite("PubSubTopicConsumerSpec") {

  val mediator = DistributedPubSub(system).mediator

  "PubSubTopicConsumer" should {
    "emit events to its sender" in {
      val topicTag = TopicTag.Master

      val receiverProbe = TestProbe("receiver")
      val reader = TestActorRef(PubSubTopicConsumer.props(topicTag))

      val expectedMsg = "Foo"

      // Receiver instructs the reader to start, linking them together
      receiverProbe.send(reader, TopicConsumer.Consume)

      mediator ! DistributedPubSubMediator.Publish(topicTag.name, expectedMsg)
      receiverProbe.expectMsg(expectedMsg)
    }
  }

}
