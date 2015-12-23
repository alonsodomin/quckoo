package io.kairos.cluster.core

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import io.kairos.JobSpec
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol.{JobAccepted, JobDisabled}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 23/12/2015.
  */
trait KairosJournal {

  def registeredJobs(implicit materializer: ActorMaterializer): Future[Map[JobId, JobSpec]]

}

object KairosJournal {

  final val CassandraReadJournalId = "akka.persistence.query.cassandra-query-journal"

  def apply(system: ActorSystem): KairosJournal = new KairosJournal {
    val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournalId)

    def registeredJobs(implicit materializer: ActorMaterializer): Future[Map[JobId, JobSpec]] = {
      readJournal.currentEventsByPersistenceId("registry", 0, System.currentTimeMillis()).
        runFold(Map.empty[JobId, JobSpec]) {
          case (map, envelope) =>
            envelope.event match {
              case JobAccepted(jobId, jobSpec) =>
                map + (jobId -> jobSpec)
              case JobDisabled(jobId) if map.contains(jobId) =>
                map - jobId
              case _ => map
            }
        }
    }
  }

}
