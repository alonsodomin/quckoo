package io.quckoo.cluster.core

import akka.actor.Props
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.cluster.topics

/**
  * Created by alonsodomin on 04/04/2016.
  */
object MasterEventPublisher {

  def props: Props =
    Props(classOf[MasterEventPublisher])

}

class MasterEventPublisher extends PubSubSubscribedEventPublisher[MasterEvent](topics.Master)
