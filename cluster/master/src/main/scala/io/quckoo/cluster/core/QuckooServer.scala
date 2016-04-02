package io.quckoo.cluster.core

import io.quckoo.api.{Registry, Scheduler}
import io.quckoo.cluster.registry.RegistryStreams
import io.quckoo.cluster.scheduler.SchedulerStreams
import io.quckoo.protocol.cluster.ClusterInfo

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait QuckooServer extends Auth with Registry with RegistryStreams with Scheduler with SchedulerStreams {

  def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo]

}
