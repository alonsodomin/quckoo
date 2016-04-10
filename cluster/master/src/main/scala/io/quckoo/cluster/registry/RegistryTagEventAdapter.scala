package io.quckoo.cluster.registry

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import io.quckoo.protocol.registry.RegistryEvent

/**
  * Created by alonsodomin on 10/04/2016.
  */
class RegistryTagEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case evt: RegistryEvent => Tagged(evt, Set(Registry.EventTag))
  }

}
