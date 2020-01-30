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

package io.quckoo.multijvm

import java.util.concurrent.ConcurrentHashMap

import akka.actor.Address
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.{Cluster, MemberStatus}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.language.implicitConversions

/**
 * Created by aalonsodominguez on 27/08/15.
 */
object MultiNodeClusterSpec {

  def clusterConfig: Config = ConfigFactory.load()

}

trait MultiNodeClusterSpec extends ScalaTestMultiNodeSpec { self: MultiNodeSpec =>

  override def initialParticipants = roles.size

  private val cachedAddresses = new ConcurrentHashMap[RoleName, Address]

  def cluster = Cluster(system)

  def clusterState: CurrentClusterState = cluster.state

  def roleName(addr: Address): Option[RoleName] = roles.find(address(_) == addr)

  /**
   * Lookup the Address for the role.
   *
   * Implicit conversion from RoleName to Address.
   *
   * It is cached, which has the implication that stopping
   * and then restarting a role (jvm) with another address is not
   * supported.
   */
  implicit def address(role: RoleName): Address = {
    cachedAddresses.get(role) match {
      case null ⇒
        val address = node(role).address
        cachedAddresses.put(role, address)
        address
      case address ⇒ address
    }
  }

  /**
   * Use this method for the initial startup of the cluster node.
   */
  def startClusterNode(): Unit = {
    if (clusterState.members.isEmpty) {
      cluster join myself
      awaitAssert(clusterState.members.map(_.address) should contain (address(myself)))
    }
  }

  /**
   * Initialize the cluster of the specified member
   * nodes (roles) and wait until all joined and `Up`.
   * First node will be started first  and others will join
   * the first.
   */
  def awaitClusterUp(roles: RoleName*): Unit = {
    runOn(roles.head) {
      // make sure that the node-to-join is started before other join
      startClusterNode()
    }
    enterBarrier(roles.head.name + "-started")
    if (roles.tail.contains(myself)) {
      cluster.join(roles.head)
    }
    if (roles.contains(myself)) {
      awaitMembersUp(numberOfMembers = roles.length)
    }
    enterBarrier(roles.map(_.name).mkString("-") + "-joined")
  }

  /**
   * Wait until the expected number of members has status Up has been reached.
   * Also asserts that nodes in the 'canNotBePartOfMemberRing' are *not* part of the cluster ring.
   */
  def awaitMembersUp(
                      numberOfMembers: Int,
                      canNotBePartOfMemberRing: Set[Address] = Set.empty,
                      timeout: FiniteDuration = 25 seconds): Unit = {
    within(timeout) {
      if (canNotBePartOfMemberRing.nonEmpty) // don't run this on an empty set
        awaitAssert(canNotBePartOfMemberRing foreach (a ⇒ clusterState.members.map(_.address) should not contain (a)))
      awaitAssert(clusterState.members.size should ===(numberOfMembers))
      awaitAssert(clusterState.members.map(_.status) should ===(Set(MemberStatus.Up)))
      // clusterView.leader is updated by LeaderChanged, await that to be updated also
      val expectedLeader = clusterState.members.headOption.map(_.address)
      awaitAssert(clusterState.leader should ===(expectedLeader))
    }
  }

}
