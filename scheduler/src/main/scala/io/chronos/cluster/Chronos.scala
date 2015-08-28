package io.chronos.cluster

import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.{Cluster, Member}
import io.chronos.cluster.protocol.WorkerProtocol
import io.chronos.protocol._
import io.chronos.registry.Registry
import io.chronos.resolver.{IvyResolve, Resolver}
import io.chronos.scheduler.Scheduler

/**
 * Created by domingueza on 24/08/15.
 */
object Chronos {

  def props(queueProps: Props)(implicit clock: Clock) =
    Props(classOf[Chronos], queueProps, clock)

  case object Shutdown

}

class Chronos(queueProps: Props)(implicit clock: Clock) extends Actor with ActorLogging {

  import Chronos._

  ClusterClientReceptionist(context.system).registerService(self)

  private val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  private val resolver = context.watch(context.actorOf(Resolver.props(IvyResolve(context.system)), "resolver"))
  private val registry = startRegistry
  context.actorOf(Props(classOf[ForwadingReceptionist], registry), "registry")

  private val scheduler = context.watch(context.actorOf(Scheduler.props(registry, queueProps), "scheduler"))

  private var clients = Set.empty[ActorRef]
  private var healthyMembers = Set.empty[Member]
  private var unreachableMembers = Set.empty[Member]

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent])
    mediator ! DistributedPubSubMediator.Subscribe(WorkerProtocol.WorkerTopic, self)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    mediator ! DistributedPubSubMediator.Unsubscribe(WorkerProtocol.WorkerTopic, self)
  }

  def clusterStatus: ClusterStatus = ClusterStatus(
    healthyMembers.map(_.uniqueAddress),
    unreachableMembers.map(_.uniqueAddress)
  )

  def receive: Receive = {
    case Connect =>
      clients += sender()
      log.info("Chronos client connected to cluster node. address={}", sender().path.address)
      sender() ! Connected

    case Disconnect =>
      clients -= sender()
      log.info("Chronos client disconnected from cluster node. address={}", sender().path.address)
      sender() ! Disconnected

    case GetClusterStatus =>
      sender() ! clusterStatus

    case MemberUp(member) =>

      healthyMembers += member
      sendStatusToClients()

    case MemberExited(member) =>
      healthyMembers -= member
      sendStatusToClients()

    case UnreachableMember(member) =>
      healthyMembers -= member
      unreachableMembers += member
      sendStatusToClients()

    case ReachableMember(member) =>
      unreachableMembers -= member
      healthyMembers += member
      sendStatusToClients()

    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

  private def startRegistry: ActorRef = if (cluster.selfRoles.contains("registry")) {
    log.info("Strating registry shards...")
    ClusterSharding(context.system).start(
      typeName        = Registry.shardName,
      entityProps     = Registry.props(resolver),
      settings        = ClusterShardingSettings(context.system).withRole("registry"),
      extractEntityId = Registry.idExtractor,
      extractShardId  = Registry.shardResolver
    )
  } else {
    log.info("Strating registry proxy...")
    ClusterSharding(context.system).startProxy(
      typeName        = Registry.shardName,
      role            = None,
      extractEntityId = Registry.idExtractor,
      extractShardId  = Registry.shardResolver
    )
  }

  private def sendStatusToClients(): Unit = {
    val status = clusterStatus
    clients.foreach { _ ! status }
  }

}
