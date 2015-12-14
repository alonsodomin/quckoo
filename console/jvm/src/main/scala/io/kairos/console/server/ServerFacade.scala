package io.kairos.console.server

import io.kairos.console.protocol.ClusterDetails
import io.kairos.console.server.security.SecurityFacade

import scala.concurrent.Future

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait ServerFacade extends SecurityFacade with RegistryFacade {

  def clusterDetails: Future[ClusterDetails]

}
