package io.chronos.cluster

import java.time.Clock

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import io.chronos.protocol.{Connect, Connected, Disconnect, Disconnected}
import io.chronos.scheduler.{Registry, Scheduler}

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
              registryProps: ActorRef => Props)(implicit clock: Clock) extends Actor {

  import Chronos._

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
    context.system.eventStream.subscribe(self, classOf[MemberEvent])
    context.system.eventStream.subscribe(self, classOf[ReachabilityEvent])
  }

  override def postStop(): Unit =
    context.system.eventStream.unsubscribe(self)

  def receive: Receive = {
    case Connect =>
      clients += sender()
      sender() ! Connected

    case Disconnect =>
      clients -= sender()
      sender() ! Disconnected

    case MemberUp(member) =>
      healthyMembers += member

    case MemberExited(member) =>
      healthyMembers -= member

    case UnreachableMember(member) =>
      healthyMembers -= member
      unreachableMembers += member

    case ReachableMember(member) =>
      unreachableMembers -= member
      healthyMembers += member

    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

}
