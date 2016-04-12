package io.quckoo.cluster

import java.util.UUID

import akka.actor.{ActorRef, Address}
import akka.cluster.{Cluster, Member, MemberStatus, UniqueAddress}
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

    def nodeId: NodeId = member.uniqueAddress.toNodeId

    def toQuckooMember: MasterNode = {
      def memberStatus = {
        if (member.status == MemberStatus.Up) NodeStatus.Active
        else NodeStatus.Unreachable
      }

      MasterNode(member.nodeId, member.address.toLocation, memberStatus)
    }

  }

  implicit class RichUniqueAddress(val uniqueAddress: UniqueAddress) extends AnyVal {

    def toNodeId: NodeId = {
      val addressUrl = s"${uniqueAddress.address.toString}#${uniqueAddress.uid}"
      UUID.nameUUIDFromBytes(addressUrl.getBytes("UTF-8"))
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
