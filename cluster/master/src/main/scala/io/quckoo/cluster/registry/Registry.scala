package io.quckoo.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import io.quckoo.JobSpec
import io.quckoo.cluster.QuckooClusterSettings
import io.quckoo.cluster.core.QuckooJournal
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._
import io.quckoo.resolver.ivy.IvyResolve

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object Registry {

  final val PersistenceId = "registry"

  def props(settings: QuckooClusterSettings) = Props(classOf[Registry], settings)

}

class Registry(settings: QuckooClusterSettings)
    extends Actor with ActorLogging with QuckooJournal {

  import Registry._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private val cluster = Cluster(context.system)
  private val shardRegion = startShardRegion

  def actorSystem = context.system

  def receive: Receive = {
    case GetJobs =>
      import context.dispatcher
      readJournal.currentEventsByPersistenceId(PersistenceId, 0, System.currentTimeMillis()).
        runFold(Map.empty[JobId, JobSpec]) {
          case (map, envelope) => envelope.event match {
            case JobAccepted(jobId, jobSpec) =>
              map + (jobId -> jobSpec)
            case JobDisabled(jobId) =>
              map + (jobId -> map(jobId).copy(disabled = true))
            case JobEnabled(jobId) =>
              map + (jobId -> map(jobId).copy(disabled = false))
            case _ => map
          }
        } pipeTo sender()

    case msg: Any =>
      shardRegion.tell(msg, sender())
  }

  private def startShardRegion: ActorRef = if (cluster.selfRoles.contains("registry")) {
    log.info("Starting registry shards...")
    ClusterSharding(context.system).start(
      typeName        = RegistryShard.ShardName,
      entityProps     = RegistryShard.props(IvyResolve(settings.ivyConfiguration)),
      settings        = ClusterShardingSettings(context.system).withRole("registry"),
      extractEntityId = RegistryShard.idExtractor,
      extractShardId  = RegistryShard.shardResolver
    )
  } else {
    log.info("Starting registry proxy...")
    ClusterSharding(context.system).startProxy(
      typeName        = RegistryShard.ShardName,
      role            = None,
      extractEntityId = RegistryShard.idExtractor,
      extractShardId  = RegistryShard.shardResolver
    )
  }

}
