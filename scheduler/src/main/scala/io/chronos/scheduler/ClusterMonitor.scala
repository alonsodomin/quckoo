package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Address}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberEvent, MemberRemoved, MemberUp}
import akka.cluster.{Cluster, MemberStatus}

/**
 * Created by aalonsodominguez on 13/07/15.
 */
class ClusterMonitor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberEvent])

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = cluster.unsubscribe(self)

  var nodes = Set.empty[Address]

  def receive = {
    case state: CurrentClusterState =>
      nodes = state.members.collect {
        case m if m.status == MemberStatus.Up => m.address
      }

    case MemberUp(member) =>
      nodes += member.address
      log.info("Member is Up: {}. {} nodes in cluster", member.address, nodes.size)

    case MemberRemoved(member, _) =>
      nodes -= member.address
      log.info("Member is Removed: {}. {} nodes cluster", member.address, nodes.size)

    case _: MemberEvent => // ignore
  }

}
