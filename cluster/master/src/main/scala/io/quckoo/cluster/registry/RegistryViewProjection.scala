package io.quckoo.cluster.registry

import akka.actor.Actor.Receive
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.stream.actor.{ActorSubscriber, MaxInFlightRequestStrategy, RequestStrategy}

/**
  * Created by alonsodomin on 13/04/2016.
  */
object RegistryViewProjection {

  private final val ReadQueueMax = 15



}

class RegistryViewProjection extends ActorSubscriber {
  import RegistryViewProjection._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator

  override def requestStrategy: RequestStrategy = new MaxInFlightRequestStrategy(max = ReadQueueMax) {
    override def inFlightInternally: Int = ReadQueueMax
  }

  override def receive: Receive = ???
}
