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

package io.quckoo.cluster.core

import java.time.Clock

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.client.ClusterClientReceptionist

import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.journal.QuckooJournal
import io.quckoo.cluster.net._
import io.quckoo.cluster.registry.Registry
import io.quckoo.cluster.scheduler.Scheduler
import io.quckoo.net.QuckooState
import io.quckoo.protocol.client._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker._

import scala.concurrent.Promise
import scala.concurrent.duration._

/**
  * Created by domingueza on 24/08/15.
  */
object QuckooGuardian {

  final val DefaultSessionTimeout: FiniteDuration = 30 minutes

  def props(settings: ClusterSettings, journal: QuckooJournal, boot: Promise[Unit])(
      implicit clock: Clock
  ): Props =
    Props(new QuckooGuardian(settings, journal, boot))

  case object Shutdown

}

class QuckooGuardian(settings: ClusterSettings, journal: QuckooJournal, boot: Promise[Unit])(
    implicit clock: Clock
) extends Actor with ActorLogging with Stash {

  import QuckooGuardian._

  //ClusterClientReceptionist(context.system).registerService(self)

  private[this] val cluster = Cluster(context.system)

  context.actorOf(UserAuthenticator.props(DefaultSessionTimeout), "authenticator")

  private[this] val registry =
    context.watch(context.actorOf(Registry.props(settings, journal), "registry"))

  private[this] val scheduler = context.watch(
    context.actorOf(
      Scheduler.props(
        settings,
        journal,
        registry
      ),
      "scheduler"
    )
  )

  private[this] var clients      = Set.empty[ActorRef]
  private[this] var clusterState = QuckooState(masterNodes = masterNodes(cluster))

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[ReachabilityEvent]
    )

    context.system.eventStream.subscribe(self, classOf[Registry.Signal])
    context.system.eventStream.subscribe(self, classOf[Scheduler.Signal])
    context.system.eventStream.subscribe(self, classOf[WorkerEvent])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    context.system.eventStream.unsubscribe(self)
  }

  def receive: Receive = starting()

  def starting(registryReady: Boolean = false, schedulerReady: Boolean = false): Receive = {
    def waitForReady: Receive = {
      case Registry.Ready =>
        if (schedulerReady) {
          becomeStarted()
        } else {
          context become starting(registryReady = true)
        }

      case Scheduler.Ready =>
        if (registryReady) {
          becomeStarted()
        } else {
          context become starting(schedulerReady = true)
        }

      case _ => stash()
    }

    defaultActivity orElse waitForReady
  }

  def started: Receive = {
    def handleUserCommands: Receive = {
      case cmd: RegistryCommand =>
        registry forward cmd

      case cmd: SchedulerCommand =>
        scheduler forward cmd

      case Shutdown =>
      // TODO Perform graceful shutdown of the cluster
    }

    defaultActivity orElse handleUserCommands
  }

  private[this] def defaultActivity: Receive = {
    case Connect =>
      clients += sender()
      log.info("Quckoo client connected to master node from address: {}", sender().path.address)
      sender() ! Connected

    case Disconnect =>
      clients -= sender()
      log.info(
        "Quckoo client disconnected from master node from address: {}",
        sender().path.address
      )
      sender() ! Disconnected

    case GetClusterStatus =>
      sender() ! clusterState

    case evt: MemberEvent =>
      evt match {
        case MemberUp(member) =>
          val event = MasterJoined(member.nodeId, member.address.toLocation)
          clusterState = clusterState.updated(event)
          context.system.eventStream.publish(event)

        case MemberRemoved(member, _) =>
          val event = MasterRemoved(member.nodeId)
          clusterState = clusterState.updated(event)
          context.system.eventStream.publish(event)

        case _ =>
      }

    case evt: ReachabilityEvent =>
      evt match {
        case ReachableMember(member) =>
          val event = MasterReachable(member.nodeId)
          clusterState = clusterState.updated(event)
          context.system.eventStream.publish(event)

        case UnreachableMember(member) =>
          val event = MasterUnreachable(member.nodeId)
          clusterState = clusterState.updated(event)
          context.system.eventStream.publish(event)
      }

    case evt: WorkerEvent =>
      clusterState = clusterState.updated(evt)

    case evt: TaskQueueUpdated =>
      clusterState = clusterState.copy(metrics = clusterState.metrics.updated(evt))
  }

  private[this] def becomeStarted(): Unit = {
    log.debug("Master node successfully started.")
    boot.success(())
    unstashAll()
    context become started
  }

}
