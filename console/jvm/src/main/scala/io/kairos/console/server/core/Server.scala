package io.kairos.console.server.core

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent
import io.kairos.api.{Auth, Registry, Scheduler}
import io.kairos.console.info.ClusterInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait Server extends Auth with Registry with Scheduler {

  def events: Source[ServerSentEvent, ActorRef]

  def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo]

}
