package io.quckoo.cluster.registry

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.quckoo.protocol.registry.RegistryEvent

/**
  * Created by alonsodomin on 28/03/2016.
  */
trait RegistryStreams {

  def registryEvents: Source[RegistryEvent, NotUsed]

}
