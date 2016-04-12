package io.quckoo.cluster.core

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.ActorMaterializer

import io.quckoo.cluster.net._
import io.quckoo.cluster.registry.Registry
import io.quckoo.cluster.scheduler.{Scheduler, TaskQueue}
import io.quckoo.cluster.{QuckooClusterSettings, topics}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.client._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker._
import io.quckoo.time.TimeSource

import scala.concurrent.duration._

/**
 * Created by domingueza on 24/08/15.
 */
object QuckooCluster {

  final val DefaultSessionTimeout: FiniteDuration = 30 minutes

  def props(settings: QuckooClusterSettings)(implicit materializer: ActorMaterializer, timeSource: TimeSource) =
    Props(classOf[QuckooCluster], settings, materializer, timeSource)

  case object Shutdown

}

class QuckooCluster(settings: QuckooClusterSettings)
                   (implicit materializer: ActorMaterializer, timeSource: TimeSource)
    extends Actor with ActorLogging with QuckooJournal {

  import QuckooCluster._

  ClusterClientReceptionist(context.system).registerService(self)

  private val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  val userAuth = context.actorOf(UserAuthenticator.props(DefaultSessionTimeout), "authenticator")

  private val registry = context.actorOf(Registry.props(settings), "registry")

  private val scheduler = context.watch(context.actorOf(
    Scheduler.props(registry, readJournal, TaskQueue.props(settings.queueMaxWorkTimeout)), "scheduler"))

  private var clients = Set.empty[ActorRef]
  private var clusterState = QuckooState(masterNodes = masterNodes(cluster))

  override implicit def actorSystem: ActorSystem = context.system

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent])
    mediator ! DistributedPubSubMediator.Subscribe(topics.Worker, self)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Worker, self)
  }

  def receive: Receive = {
    case Connect =>
      clients += sender()
      log.info("Quckoo client connected to cluster node. address={}", sender().path.address)
      sender() ! Connected

    case Disconnect =>
      clients -= sender()
      log.info("Quckoo client disconnected from cluster node. address={}", sender().path.address)
      sender() ! Disconnected

    case GetClusterStatus =>
      sender() ! clusterState

    case cmd: RegistryCommand =>
      registry.tell(cmd, sender())

    case cmd: SchedulerCommand =>
      scheduler.tell(cmd, sender())

    case evt: MemberEvent => evt match {
      case MemberUp(member) =>
        val event = MasterJoined(member.nodeId, member.address.toLocation)
        clusterState = clusterState.updated(event)
        mediator ! DistributedPubSubMediator.Publish(topics.Master, event)

      case MemberRemoved(member, _) =>
        val event = MasterRemoved(member.nodeId)
        clusterState = clusterState.updated(event)
        mediator ! DistributedPubSubMediator.Publish(topics.Master, event)

      case _ =>
    }

    case evt: ReachabilityEvent => evt match {
      case ReachableMember(member) =>
        val event = MasterReachable(member.nodeId)
        clusterState = clusterState.updated(event)
        mediator ! DistributedPubSubMediator.Publish(topics.Master, event)

      case UnreachableMember(member) =>
        val event = MasterUnreachable(member.nodeId)
        clusterState = clusterState.updated(event)
        mediator ! DistributedPubSubMediator.Publish(topics.Master, event)
    }

    case evt: WorkerJoined =>
      clusterState = clusterState.updated(evt)

    case evt: WorkerRemoved =>
      clusterState = clusterState.updated(evt)

    case evt: TaskQueueUpdated =>
      clusterState = clusterState.copy(metrics = clusterState.metrics.updated(evt))

    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

}
