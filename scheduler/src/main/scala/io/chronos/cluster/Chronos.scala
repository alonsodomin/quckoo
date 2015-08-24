package io.chronos.cluster

import java.time.Clock

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
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
    typeName = Registry.shardName,
    entityProps = registryProps(resolver),
    settings = shardSettings,
    extractEntityId = Registry.idExtractor,
    extractShardId = Registry.shardResolver
  )
  context.actorOf(Props(classOf[ForwadingReceptionist], registry), "registry")

  private val scheduler = context.watch(context.actorOf(Scheduler.props(shardSettings, registry, queueProps), "scheduler"))

  def receive: Receive = {
    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

}
