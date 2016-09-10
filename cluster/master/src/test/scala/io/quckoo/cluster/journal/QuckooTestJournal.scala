package io.quckoo.cluster.journal

import akka.actor.ActorSystem
import akka.persistence.inmemory.query.scaladsl.InMemoryReadJournal

/**
  * Created by alonsodomin on 10/09/2016.
  */
class QuckooTestJournal(implicit val actorSystem: ActorSystem) extends QuckooJournal {
  type ReadRepr = InMemoryReadJournal

  protected val journalId = InMemoryReadJournal.Identifier
}

