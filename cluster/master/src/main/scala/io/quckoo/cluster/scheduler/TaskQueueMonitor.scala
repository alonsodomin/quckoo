package io.quckoo.cluster.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, PNCounterMap, Replicator}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import io.quckoo.cluster.core.PubSubSubscribedEventPublisher
import io.quckoo.cluster.topics
import io.quckoo.protocol.cluster.MasterRemoved
import io.quckoo.protocol.scheduler.TaskQueueUpdated

import scala.collection.immutable.Queue

/**
  * Created by alonsodomin on 12/04/2016.
  */

object TaskQueueMonitor {

  case class QueueMetrics(pendingPerNode: Map[String, Int] = Map.empty,
                          inProgressPerNode: Map[String, Int] = Map.empty)

  def props: Props = Props(classOf[TaskQueueMonitor])

}

class TaskQueueMonitor extends Actor with ActorLogging {
  import TaskQueueMonitor._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator
  private[this] val mediator = DistributedPubSub(context.system).mediator

  private[this] var currentMetrics = QueueMetrics()
  private[this] var stash: Queue[Any] = Queue.empty

  override def preStart(): Unit = {
    replicator ! Replicator.Subscribe(TaskQueue.PendingKey, self)
    replicator ! Replicator.Subscribe(TaskQueue.InProgressKey, self)
    mediator ! DistributedPubSubMediator.Subscribe(topics.Master, self)
  }

  override def receive = initialising

  private def initialising: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      log.info("Task monitor initialised in node: {}", cluster.selfUniqueAddress.address.hostPort)
      stash.foreach(self ! _)
      stash = Queue.empty
      context.become(ready)

    case msg: Any =>
      // Stash any message during initialization
      stash = stash.enqueue(msg)
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
      replicator ! Replicator.Update(TaskQueue.PendingKey, PNCounterMap(), Replicator.WriteLocal) {
        _ - nodeId.toString
      }
      // TODO This might not be the right thing to do with that tasks that are in-progress
      // ideally, the worker that has got it should be able to notify any of the partitions
      // that conform the cluster-wide queue
      replicator ! Replicator.Update(TaskQueue.InProgressKey, PNCounterMap(), Replicator.WriteLocal) {
        _ - nodeId.toString
      }
  }

  private def publishMetrics(): Unit = {
    val totalPending = currentMetrics.pendingPerNode.values.sum
    val totalInProgress = currentMetrics.inProgressPerNode.values.sum
    mediator ! DistributedPubSubMediator.Publish(
      topics.Master,
      TaskQueueUpdated(totalPending, totalInProgress)
    )
  }

}

final class TaskQueueEventPublisher extends PubSubSubscribedEventPublisher[TaskQueueUpdated](topics.Master)