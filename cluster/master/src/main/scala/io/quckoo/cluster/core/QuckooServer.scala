package io.quckoo.cluster.core

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent
import io.quckoo.api.{Registry, Scheduler}
import io.quckoo.cluster.registry.RegistryStreams
import io.quckoo.protocol.cluster.ClusterInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait QuckooServer extends Auth with Registry with RegistryStreams with Scheduler {

  def events: Source[ServerSentEvent, NotUsed]

  def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo]

}
