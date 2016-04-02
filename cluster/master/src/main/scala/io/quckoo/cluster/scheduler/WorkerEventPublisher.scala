package io.quckoo.cluster.scheduler

import akka.actor.Props
import io.quckoo.cluster.core.PubSubSubscribedEventPublisher
import io.quckoo.protocol.topics
import io.quckoo.protocol.worker.WorkerEvent

/**
  * Created by alonsodomin on 01/04/2016.
  */
object WorkerEventPublisher {

  def props: Props =
    Props(classOf[WorkerEventPublisher])

}

class WorkerEventPublisher extends PubSubSubscribedEventPublisher[WorkerEvent](topics.WorkerTopic)
