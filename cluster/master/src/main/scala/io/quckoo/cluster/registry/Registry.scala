package io.quckoo.cluster.registry

import scala.concurrent._

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.scaladsl.Source
import io.quckoo.JobSpec
import io.quckoo.id.JobId
import io.quckoo.cluster.QuckooClusterSettings
import io.quckoo.cluster.core.QuckooJournal
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Resolver
import io.quckoo.resolver.ivy.IvyResolve

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object Registry {

  final val EventTag = "registry"

  def props(settings: QuckooClusterSettings) = Props(classOf[Registry], settings)

}

class Registry(settings: QuckooClusterSettings)
    extends Actor with ActorLogging with QuckooJournal {

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private[this] val cluster = Cluster(context.system)
  private[this] val resolver = context.actorOf(
    Resolver.props(IvyResolve(settings.ivyConfiguration)).withDispatcher("quckoo.resolver.dispatcher"),
    "resolver")
  private[this] val shardRegion = startShardRegion
  private[this] val index = context.actorOf(RegistryIndex.props(shardRegion), s"index")

  def actorSystem = context.system

  override def preStart(): Unit = {
    // Restart the indexes for the registry partitions
    readJournal.currentPersistenceIds().
      filter(_.startsWith(JobAggregate.PersistenceIdPrefix)).
      runForeach { partitionId =>
        index ! RegistryIndex.IndexJob(partitionId)
      }
  }

  def receive: Receive = {
    case GetJobs =>
      import context.dispatcher
      val origSender = sender()
      queryJobs pipeTo origSender

    case msg: GetJob =>
      index forward msg

    case msg: RegistryWriteCommand =>
      shardRegion forward msg
  }

  private def queryJobs: Future[Map[JobId, JobSpec]] = {
    Source.actorRef[(JobId, JobSpec)](10, OverflowStrategy.fail).
      mapMaterializedValue { idsStream =>
        index.tell(GetJobs, idsStream)
      }.runFold(Map.empty[JobId, JobSpec]) {
        case (map, (jobId, jobSpec)) =>
          map + (jobId -> jobSpec)
      }
  }

  private def startShardRegion: ActorRef = if (cluster.selfRoles.contains("registry")) {
    log.info("Starting registry shards...")
    ClusterSharding(context.system).start(
      typeName        = JobAggregate.ShardName,
      entityProps     = JobAggregate.props,
      settings        = ClusterShardingSettings(context.system).withRole("registry"),
      extractEntityId = JobAggregate.idExtractor,
      extractShardId  = JobAggregate.shardResolver
    )
  } else {
    log.info("Starting registry proxy...")
    ClusterSharding(context.system).startProxy(
      typeName        = JobAggregate.ShardName,
      role            = None,
      extractEntityId = JobAggregate.idExtractor,
      extractShardId  = JobAggregate.shardResolver
    )
  }

}
