package io.kairos.cluster.core

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import akka.cluster.client.ClusterClientReceptionist
import akka.pattern._
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import io.kairos.JobSpec
import io.kairos.cluster.registry.RegistryView
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol.{JobDisabled, JobRejected, JobAccepted, GetJobs}

/**
 * Created by aalonsodominguez on 24/08/15.
 */
private[cluster] class RegistryReceptionist(registryShardingRef: ActorRef) extends Actor with ActorLogging {

  ClusterClientReceptionist(context.system).registerService(self)

  val registryView = context.actorOf(Props[RegistryView], "view")

  def receive: Receive = {
    case GetJobs =>
      val readJournal = PersistenceQuery(context.system).readJournalFor[CassandraReadJournal](
        "akka.persistence.query.cassandra-query-journal"
      )

      import context.dispatcher
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
        } pipeTo sender()

    case msg: Any =>
      registryShardingRef.tell(msg, sender())
  }

}
