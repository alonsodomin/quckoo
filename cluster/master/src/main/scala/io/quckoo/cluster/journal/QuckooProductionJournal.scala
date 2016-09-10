package io.quckoo.cluster.journal

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal

/**
  * Created by alonsodomin on 10/09/2016.
  */
class QuckooProductionJournal(implicit val actorSystem: ActorSystem) extends QuckooJournal {
  type ReadRepr = CassandraReadJournal

  protected val journalId = CassandraReadJournal.Identifier
}
