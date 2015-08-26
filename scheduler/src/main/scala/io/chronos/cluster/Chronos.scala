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
import io.chronos.scheduler.Scheduler

/**
 * Created by domingueza on 24/08/15.
 */
object Chronos {

  def props(shardSettings: ClusterShardingSettings,
            resolverProps: Props,
            queueProps: Props)(registryProps: ActorRef => Props)(implicit clock: Clock) =
    Props(classOf[Chronos], shardSettings, resolverProps, queueProps, registryProps, clock)

  case object Shutdown

}

class Chronos(shardSettings: ClusterShardingSettings,
              resolverProps: Props,
              queueProps: Props,
              registryProps: ActorRef => Props)(implicit clock: Clock) extends Actor with ActorLogging {

  import Chronos._

  ClusterClientReceptionist(context.system).registerService(self)

  private val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  private val resolver = context.watch(context.actorOf(resolverProps, "resolver"))
  private val registry = ClusterSharding(context.system).start(
    typeName        = Registry.shardName,
    entityProps     = registryProps(resolver),
    settings        = shardSettings,
    extractEntityId = Registry.idExtractor,
    extractShardId  = Registry.shardResolver
  )
  context.actorOf(Props(classOf[ForwadingReceptionist], registry), "registry")

  private val scheduler = context.watch(context.actorOf(Scheduler.props(shardSettings, registry, queueProps), "scheduler"))

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

  private def sendStatusToClients(): Unit = {
    val status = clusterStatus
    clients.foreach { _ ! status }
  }

}
