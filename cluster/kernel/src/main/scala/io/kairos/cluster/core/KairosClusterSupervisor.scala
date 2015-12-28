package io.kairos.cluster.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.stream.ActorMaterializer
import io.kairos.cluster.protocol._
import io.kairos.cluster.registry.Registry
import io.kairos.cluster.scheduler.{Scheduler, TaskQueue}
import io.kairos.cluster.{KairosClusterSettings, KairosStatus}
import io.kairos.protocol._
import io.kairos.resolver.Resolver
import io.kairos.resolver.ivy.IvyResolve
import io.kairos.time.TimeSource

/**
 * Created by domingueza on 24/08/15.
 */
object KairosClusterSupervisor {

  def props(settings: KairosClusterSettings)(implicit materializer: ActorMaterializer, timeSource: TimeSource) =
    Props(classOf[KairosClusterSupervisor], settings, materializer, timeSource)

  case object Shutdown

}

class KairosClusterSupervisor(settings: KairosClusterSettings)
                             (implicit materializer: ActorMaterializer, timeSource: TimeSource) extends Actor with ActorLogging {

  import ClientProtocol._
  import KairosClusterSupervisor._
  import WorkerProtocol.{WorkerJoined, WorkerRemoved}

  ClusterClientReceptionist(context.system).registerService(self)

  private val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  private val resolver = context.watch(context.actorOf(
    Resolver.props(new IvyResolve(settings.ivyConfiguration)), "resolver"))
  private val registry = startRegistry
  context.actorOf(RegistryReceptionist.props(registry), "registry")

  private val scheduler = context.watch(context.actorOf(
    Scheduler.props(registry, TaskQueue.props(settings.queueMaxWorkTimeout)), "scheduler"))

  private var clients = Set.empty[ActorRef]
  private var kairosStatus = KairosStatus()

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent])
    mediator ! DistributedPubSubMediator.Subscribe(WorkerProtocol.WorkerTopic, self)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    mediator ! DistributedPubSubMediator.Unsubscribe(WorkerProtocol.WorkerTopic, self)
  }

  def receive: Receive = {
    case Connect =>
      clients += sender()
      log.info("Kairos client connected to cluster node. address={}", sender().path.address)
      sender() ! Connected

    case Disconnect =>
      clients -= sender()
      log.info("Kairos client disconnected from cluster node. address={}", sender().path.address)
      sender() ! Disconnected

    case GetClusterStatus =>
      sender() ! kairosStatus

    case evt: MemberEvent =>
      kairosStatus = kairosStatus.update(evt)

    case WorkerJoined(_) =>
      kairosStatus = kairosStatus.copy(workers = kairosStatus.workers + 1)

    case WorkerRemoved(_) =>
      kairosStatus = kairosStatus.copy(workers = kairosStatus.workers - 1)

    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

  private def startRegistry: ActorRef = if (cluster.selfRoles.contains("registry")) {
    log.info("Starting registry shards...")
    ClusterSharding(context.system).start(
      typeName        = Registry.shardName,
      entityProps     = Registry.props(resolver),
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
