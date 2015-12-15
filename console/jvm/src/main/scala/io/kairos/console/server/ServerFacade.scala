package io.kairos.console.server

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent
import io.kairos.console.info.ClusterInfo
import io.kairos.console.server.security.SecurityFacade

import scala.concurrent.Future

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ServerFacade extends SecurityFacade with RegistryFacade {

  def events: Source[ServerSentEvent, ActorRef]

  def clusterDetails: Future[ClusterInfo]

}
