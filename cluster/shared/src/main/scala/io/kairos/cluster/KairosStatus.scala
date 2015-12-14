package io.kairos.cluster

import akka.cluster.ClusterEvent.{MemberExited, MemberUp, MemberEvent}

/**
 * Created by aalonsodominguez on 24/08/15.
 */
case class KairosStatus(members: Map[Int, KairosMember] = Map.empty, workers: Int = 0) {

  def update(event: MemberEvent): KairosStatus = event match {
    case MemberUp(member) =>
      copy(members + (member.uniqueAddress.uid -> KairosMember(member.uniqueAddress.address)))

    case MemberExited(member) =>
      copy(members - member.uniqueAddress.uid)

    case _ => this
  }

}
