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

package io.quckoo.cluster.scheduler

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, PNCounterMap, Replicator}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import io.quckoo.api.TopicTag
import io.quckoo.protocol.cluster.MasterRemoved
import io.quckoo.protocol.scheduler.TaskQueueUpdated

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 12/04/2016.
  */
object TaskQueueMonitor {

  case class QueueMetrics(
      pendingPerNode: Map[String, Int] = Map.empty,
      inProgressPerNode: Map[String, Int] = Map.empty
  )

  def props: Props = Props(new TaskQueueMonitor)

}

class TaskQueueMonitor extends Actor with ActorLogging with Stash {
  import TaskQueueMonitor._

  val timeout = 5 seconds

  implicit val cluster         = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator
  private[this] val mediator   = DistributedPubSub(context.system).mediator

  private[this] var currentMetrics = QueueMetrics()

  override def preStart(): Unit = {
    replicator ! Replicator.Subscribe(TaskQueue.PendingKey, self)
    replicator ! Replicator.Subscribe(TaskQueue.InProgressKey, self)
    mediator ! DistributedPubSubMediator.Subscribe(TopicTag.Master.name, self)
  }

  override def receive: Receive = initialising

  private def initialising: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      log.info("Task monitor initialised in node: {}", cluster.selfUniqueAddress.address.hostPort)
      unstashAll()
      context.become(ready)

    case _ => stash()
  }

  private def ready: Receive = {
    case evt @ Replicator.Changed(TaskQueue.PendingKey) =>
      val state = evt.get(TaskQueue.PendingKey).entries.map {
        case (node, value) => node -> value.toInt
      }
      currentMetrics = currentMetrics.copy(pendingPerNode = state)
      publishMetrics()

    case evt @ Replicator.Changed(TaskQueue.InProgressKey) =>
      val state = evt.get(TaskQueue.InProgressKey).entries.map {
        case (node, value) => node -> value.toInt
      }
      currentMetrics = currentMetrics.copy(inProgressPerNode = state)
      publishMetrics()

    case MasterRemoved(nodeId) =>
      // Drop the key holding the counter for the lost node.
      replicator ! Replicator
        .Update(TaskQueue.PendingKey, PNCounterMap[String](), Replicator.WriteMajority(timeout)) {
          _ - nodeId.toString
        }
      // TODO This might not be the right thing to do with that tasks that are in-progress
      // ideally, the worker that has got it should be able to notify any of the partitions
      // that conform the cluster-wide queue
      replicator ! Replicator
        .Update(TaskQueue.InProgressKey, PNCounterMap[String](), Replicator.WriteMajority(timeout)) {
          _ - nodeId.toString
        }
  }

  private def publishMetrics(): Unit = {
    val totalPending    = currentMetrics.pendingPerNode.values.sum
    val totalInProgress = currentMetrics.inProgressPerNode.values.sum
    mediator ! DistributedPubSubMediator.Publish(
      TopicTag.Master.name,
      TaskQueueUpdated(totalPending, totalInProgress)
    )
  }

}
