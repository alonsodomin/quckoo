package io.quckoo.cluster.registry

import akka.actor.Props
import io.quckoo.cluster.core.PubSubSubscribedEventPublisher
import io.quckoo.cluster.topics
import io.quckoo.protocol.registry.RegistryEvent

/**
  * Created by alonsodomin on 28/03/2016.
  */
object RegistryEventPublisher {

  def props: Props = Props(classOf[RegistryEventPublisher])

}
class RegistryEventPublisher extends PubSubSubscribedEventPublisher[RegistryEvent](topics.Registry)
