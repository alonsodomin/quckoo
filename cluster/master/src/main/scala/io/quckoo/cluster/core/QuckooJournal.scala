package io.quckoo.cluster.core

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery

/**
  * Created by alonsodomin on 23/12/2015.
  */
trait QuckooJournal {
  import QuckooJournal._

  implicit def actorSystem: ActorSystem

  val readJournal = PersistenceQuery(actorSystem).
    readJournalFor[CassandraReadJournal](CassandraReadJournalId)

}

object QuckooJournal {

  final val CassandraReadJournalId = "cassandra-query-journal"

}
