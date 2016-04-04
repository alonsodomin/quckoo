package io.quckoo.cluster.core

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.worker.WorkerEvent

/**
  * Created by alonsodomin on 04/04/2016.
  */
trait ClusterStreams {

  def masterEvents: Source[MasterEvent, NotUsed]

  def workerEvents: Source[WorkerEvent, NotUsed]

}
