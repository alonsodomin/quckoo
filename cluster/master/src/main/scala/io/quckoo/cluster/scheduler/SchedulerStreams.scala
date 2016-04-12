package io.quckoo.cluster.scheduler

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import io.quckoo.protocol.scheduler.TaskQueueUpdated
import io.quckoo.protocol.worker.WorkerEvent

/**
  * Created by alonsodomin on 01/04/2016.
  */
trait SchedulerStreams {

  def queueMetrics: Source[TaskQueueUpdated, NotUsed]

}
