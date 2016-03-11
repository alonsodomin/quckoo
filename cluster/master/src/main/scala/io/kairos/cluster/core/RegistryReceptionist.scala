package io.kairos.cluster.core

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import io.kairos.JobSpec
import io.kairos.cluster.KairosClusterSettings
import io.kairos.cluster.registry.{Registry, RegistryView}
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol.{JobDisabled, JobRejected, JobAccepted, GetJobs}
import io.kairos.resolver.ivy.IvyResolve

/**
 * Created by aalonsodominguez on 24/08/15.
 */
private[cluster] object RegistryReceptionist {

  def props(settings: KairosClusterSettings)(implicit materializer: ActorMaterializer) =
    Props(classOf[RegistryReceptionist], settings, materializer)

}

private[cluster] class RegistryReceptionist(settings: KairosClusterSettings)(implicit materializer: ActorMaterializer)
  extends Actor with ActorLogging {

  ClusterClientReceptionist(context.system).registerService(self)

  private val cluster = Cluster(context.system)
  private val shardRegion = startShardRegion

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
      shardRegion.tell(msg, sender())
  }

  private def startShardRegion: ActorRef = if (cluster.selfRoles.contains("registry")) {
    log.info("Starting registry shards...")
    ClusterSharding(context.system).start(
      typeName        = Registry.shardName,
      entityProps     = Registry.props(IvyResolve(settings.ivyConfiguration)),
      settings        = ClusterShardingSettings(context.system).withRole("registry"),
      extractEntityId = Registry.idExtractor,
      extractShardId  = Registry.shardResolver
    )
  } else {
    log.info("Starting registry proxy...")
    ClusterSharding(context.system).startProxy(
      typeName        = Registry.shardName,
      role            = None,
      extractEntityId = Registry.idExtractor,
      extractShardId  = Registry.shardResolver
    )
  }

}
