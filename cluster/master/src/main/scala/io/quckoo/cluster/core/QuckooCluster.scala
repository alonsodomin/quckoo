package io.quckoo.cluster.core

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.stream.ActorMaterializer
import io.quckoo.cluster.protocol._
import io.quckoo.cluster.registry.{Registry, RegistryShard}
import io.quckoo.cluster.scheduler.{Scheduler, TaskQueue}
import io.quckoo.cluster.{QuckooClusterSettings, KairosStatus}
import io.quckoo.protocol.topics
import io.quckoo.protocol.client._
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
  private var clusterStatus = KairosStatus()

  override implicit def actorSystem: ActorSystem = context.system

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent])
    mediator ! DistributedPubSubMediator.Subscribe(topics.WorkerTopic, self)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.WorkerTopic, self)
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
      sender() ! clusterStatus

    case cmd: RegistryCommand =>
      registry.tell(cmd, sender())

    case cmd: SchedulerCommand =>
      scheduler.tell(cmd, sender())

    case evt: MemberEvent =>
      clusterStatus = clusterStatus.update(evt)

    case WorkerJoined(_) =>
      clusterStatus = clusterStatus.copy(workers = clusterStatus.workers + 1)

    case WorkerRemoved(_) =>
      clusterStatus = clusterStatus.copy(workers = clusterStatus.workers - 1)

    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

}
