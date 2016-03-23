package io.quckoo.console.server.core

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent
import io.quckoo.api.{Auth, Registry, Scheduler}
import io.quckoo.console.info.ClusterInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait Server extends Auth with Registry with Scheduler {

  def events: Source[ServerSentEvent, ActorRef]

  def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo]

}
