/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster

import java.util.UUID

import akka.actor.{ActorRef, Address}
import akka.cluster.{Cluster, Member, MemberStatus, UniqueAddress}

import io.quckoo.NodeId
import io.quckoo.net._

/**
  * Created by alonsodomin on 03/04/2016.
  */
package object net {

  def masterNodes(cluster: Cluster): Map[NodeId, MasterNode] =
    cluster.state.members.map(_.toQuckooMember).map(member => member.id -> member).toMap

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
      val addressUrl = s"${uniqueAddress.address.toString}#${uniqueAddress.longUid}"
      NodeId(UUID.nameUUIDFromBytes(addressUrl.getBytes("UTF-8")))
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
