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

package io.quckoo.cluster.core

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source

import io.quckoo.api.TopicTag

/**
  * Created by domingueza on 28/02/2017.
  */
object Topic {

  def source[A](implicit actorSystem: ActorSystem, topicTag: TopicTag[A]): Source[A, NotUsed] = {
    def isLocal: Boolean = topicTag match {
      case TopicTag.Registry | TopicTag.Scheduler => false
      case _                                      => true
    }

    def consumerProps: Props = {
      if (isLocal) LocalTopicConsumer.props[A]
      else PubSubTopicConsumer.props[A]
    }

    val consumerRef = actorSystem.actorOf(consumerProps)
    Source.actorRef[A](50, OverflowStrategy.dropTail)
      .mapMaterializedValue { upstream =>
        consumerRef.tell(TopicConsumer.Consume, upstream)
        NotUsed
      }
  }

}
