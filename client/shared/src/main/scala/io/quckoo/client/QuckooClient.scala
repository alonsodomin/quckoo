package io.quckoo.client

import io.quckoo.api.{Cluster, Registry, Scheduler}
import io.quckoo.auth.User
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.worker.WorkerEvent
import monifu.reactive.Observable

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
trait QuckooClient extends Cluster with Registry with Scheduler {

  def registryEvents: Observable[RegistryEvent]

  def masterEvents: Observable[MasterEvent]

  def workerEvents: Observable[WorkerEvent]

  def principal: Option[User]

  def close()(implicit ec: ExecutionContext): Future[Unit]

}
