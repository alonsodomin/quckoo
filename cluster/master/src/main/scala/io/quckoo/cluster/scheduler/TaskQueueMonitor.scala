package io.quckoo.cluster.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, PNCounterMap, Replicator}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import io.quckoo.cluster.core.PubSubSubscribedEventPublisher
import io.quckoo.cluster.topics
import io.quckoo.protocol.cluster.MasterRemoved
import io.quckoo.protocol.scheduler.TaskQueueUpdated

/**
  * Created by alonsodomin on 12/04/2016.
  */

object TaskQueueMonitor {

  case class QueueMetrics(pendingPerNode: Map[String, Int] = Map.empty)

  def props: Props = Props(classOf[TaskQueueMonitor])

}

class TaskQueueMonitor extends Actor with ActorLogging {
  import TaskQueueMonitor._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator
  private[this] val mediator = DistributedPubSub(context.system).mediator

  private[this] var currentMetrics = QueueMetrics()

  override def preStart(): Unit = {
    replicator ! Replicator.Subscribe(TaskQueue.QueueSizeKey, self)
    mediator ! DistributedPubSubMediator.Subscribe(topics.Master, self)
  }

  override def receive = initialising

  private def initialising: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      log.info("Task monitor initialised in node: {}", self.path.address.hostPort)
      context.become(ready)
  }

  private def ready: Receive = {
    case evt @ Replicator.Changed(TaskQueue.QueueSizeKey) =>
      val state = evt.get(TaskQueue.QueueSizeKey).entries.map {
        case (node, value) => node -> value.toInt
      }
      currentMetrics = currentMetrics.copy(pendingPerNode = state)
      val total = currentMetrics.pendingPerNode.values.sum
      mediator ! DistributedPubSubMediator.Publish(topics.Master, TaskQueueUpdated(total))

    case MasterRemoved(nodeId) =>
      // Drop the key holding the counter for the lost node.
      replicator ! Replicator.Update(TaskQueue.QueueSizeKey, PNCounterMap(), Replicator.WriteLocal) {
        _ - nodeId.toString
      }
  }

}

final class TaskQueueEventPublisher extends PubSubSubscribedEventPublisher[TaskQueueUpdated](topics.Master)
