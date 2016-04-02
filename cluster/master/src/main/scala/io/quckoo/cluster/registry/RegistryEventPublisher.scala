package io.quckoo.cluster.registry

import akka.actor.Props
import io.quckoo.cluster.core.PubSubSubscribedEventPublisher
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.topics

/**
  * Created by alonsodomin on 28/03/2016.
  */
object RegistryEventPublisher {

  def props: Props = Props(classOf[RegistryEventPublisher])

}
class RegistryEventPublisher extends PubSubSubscribedEventPublisher[RegistryEvent](topics.RegistryTopic)
