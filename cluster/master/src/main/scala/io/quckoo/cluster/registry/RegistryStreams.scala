package io.quckoo.cluster.registry

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import io.quckoo.protocol.registry.RegistryEvent

/**
  * Created by alonsodomin on 28/03/2016.
  */
trait RegistryStreams {

  def registryEvents: Source[RegistryEvent, ActorRef]

}
