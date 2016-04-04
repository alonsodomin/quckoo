package io.quckoo.cluster

import java.util.UUID

import akka.actor.{ActorRef, Address}
import akka.cluster.{Cluster, Member, MemberStatus}
import io.quckoo.id.NodeId
import io.quckoo.net._

/**
  * Created by alonsodomin on 03/04/2016.
  */
package object net {

  def masterNodes(cluster: Cluster): Map[NodeId, MasterNode] =
    cluster.state.members.map(_.toQuckooMember).
      map(member => member.id -> member).
      toMap

  implicit class RichMember(val member: Member) extends AnyVal {

    def nodeId: NodeId = {
      val addressUrl = s"${member.uniqueAddress.address.toString}#${member.uniqueAddress.uid}"
      UUID.nameUUIDFromBytes(addressUrl.getBytes("UTF-8"))
    }

    def toQuckooMember: MasterNode = {
      def memberStatus = {
        if (member.status == MemberStatus.Up) NodeStatus.Active
        else NodeStatus.Unreachable
      }

      MasterNode(member.nodeId, member.address.toLocation, memberStatus)
    }

  }

  implicit class RichAddress(val address: Address) extends AnyVal {

    def toLocation: Location =
      Location(address.host.getOrElse("localhost"))

  }

  implicit class RichActorRef(val actorRef: ActorRef) extends AnyVal {

    def location: Location = actorRef.path.address.toLocation

  }

}
