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

import akka.actor.PoisonPill
import akka.testkit.{TestActorRef, TestProbe}

import io.quckoo.NodeId
import io.quckoo.api.TopicTag
import io.quckoo.protocol.cluster.MasterUnreachable
import io.quckoo.testkit.QuckooActorSuite

/**
  * Created by alonsodomin on 01/03/2017.
  */
class LocalTopicConsumerSpec extends QuckooActorSuite("LocalTopicConsumerSpec") {

  "LocalTopicConsumer" should {
    "emit events to its sender" in {
      val topicTag = TopicTag.Master

      val receiverProbe = TestProbe("receiver")
      val consumer = TestActorRef(LocalTopicConsumer.props(topicTag))

      val expectedMsg = MasterUnreachable(NodeId(UUID.randomUUID()))

      // Receiver instructs the reader to start, linking them together
      receiverProbe.send(consumer, TopicConsumer.Consume)

      system.eventStream.publish(expectedMsg)
      receiverProbe.expectMsg(expectedMsg)

      watch(consumer)
      consumer ! PoisonPill
      expectTerminated(consumer)
    }
  }

}
