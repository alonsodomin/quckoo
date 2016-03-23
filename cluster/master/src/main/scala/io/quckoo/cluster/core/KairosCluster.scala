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
import io.quckoo.cluster.{KairosClusterSettings, KairosStatus}
import io.quckoo.protocol._
import io.quckoo.resolver.ivy.IvyResolve
import io.quckoo.time.TimeSource

import scala.concurrent.duration._

/**
 * Created by domingueza on 24/08/15.
 */
object KairosCluster {

  final val DefaultSessionTimeout: FiniteDuration = 30 minutes

  def props(settings: KairosClusterSettings)(implicit materializer: ActorMaterializer, timeSource: TimeSource) =
    Props(classOf[KairosCluster], settings, materializer, timeSource)

  case object Shutdown

}

class KairosCluster(settings: KairosClusterSettings)
                   (implicit materializer: ActorMaterializer, timeSource: TimeSource)
    extends Actor with ActorLogging with KairosJournal {

  import ClientProtocol._
  import KairosCluster._
  import WorkerProtocol.{WorkerJoined, WorkerRemoved}

  ClusterClientReceptionist(context.system).registerService(self)

  private val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  val userAuth = context.actorOf(UserAuthenticator.props(DefaultSessionTimeout), "authenticator")

  private val registry = context.actorOf(Registry.props(settings), "registry")

  private val scheduler = context.watch(context.actorOf(
    Scheduler.props(registry, readJournal, TaskQueue.props(settings.queueMaxWorkTimeout)), "scheduler"))

  private var clients = Set.empty[ActorRef]
  private var kairosStatus = KairosStatus()

  override implicit def actorSystem: ActorSystem = context.system

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent])
    mediator ! DistributedPubSubMediator.Subscribe(WorkerProtocol.WorkerTopic, self)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    mediator ! DistributedPubSubMediator.Unsubscribe(WorkerProtocol.WorkerTopic, self)
  }

  def receive: Receive = {
    case Connect =>
      clients += sender()
      log.info("Kairos client connected to cluster node. address={}", sender().path.address)
      sender() ! Connected

    case Disconnect =>
      clients -= sender()
      log.info("Kairos client disconnected from cluster node. address={}", sender().path.address)
      sender() ! Disconnected

    case GetClusterStatus =>
      sender() ! kairosStatus

    case cmd: RegistryProtocol.RegistryCommand =>
      registry.tell(cmd, sender())

    case cmd: SchedulerProtocol.SchedulerCommand =>
      scheduler.tell(cmd, sender())

    case evt: MemberEvent =>
      kairosStatus = kairosStatus.update(evt)

    case WorkerJoined(_) =>
      kairosStatus = kairosStatus.copy(workers = kairosStatus.workers + 1)

    case WorkerRemoved(_) =>
      kairosStatus = kairosStatus.copy(workers = kairosStatus.workers - 1)

    case Shutdown =>
      // Perform graceful shutdown of the cluster
  }

}
