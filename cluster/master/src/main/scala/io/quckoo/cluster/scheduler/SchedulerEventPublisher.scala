package io.quckoo.cluster.scheduler

import akka.actor.Props
import io.quckoo.cluster.core.PubSubSubscribedEventPublisher
import io.quckoo.cluster.topics
import io.quckoo.protocol.scheduler.SchedulerEvent

/**
  * Created by alonsodomin on 11/07/2016.
  */
object SchedulerEventPublisher {

  def props: Props = Props(classOf[SchedulerEventPublisher])

}

class SchedulerEventPublisher extends PubSubSubscribedEventPublisher[SchedulerEvent](topics.Scheduler)
