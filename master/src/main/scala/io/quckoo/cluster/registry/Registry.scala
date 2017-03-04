/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.cluster.registry

import java.util.UUID

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.persistence.query.EventEnvelope2
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import io.quckoo.{JobId, JobNotFound, JobSpec}
import io.quckoo.api.TopicTag
import io.quckoo.cluster.QuckooRoles
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.journal.QuckooJournal
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Resolver
import io.quckoo.resolver.ivy.IvyResolve

import scala.concurrent._
import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 24/08/15.
  */
object Registry {

  final val EventTag = "registry"

  private[registry] object WarmUp {
    case object Start
    case object Ack
    case object Completed
    final case class Failed(exception: Throwable)
  }

  sealed trait Signal
  case object Ready extends Signal

  def props(settings: ClusterSettings, journal: QuckooJournal): Props = {
    val resolve       = IvyResolve(settings.resolver)
    val resolverProps = Resolver.props(resolve).withDispatcher("quckoo.resolver.dispatcher")
    props(RegistrySettings(resolverProps), journal)
  }

  def props(settings: RegistrySettings, journal: QuckooJournal): Props =
    Props(new Registry(settings, journal))

  private[registry] def startShardRegion(system: ActorSystem): ActorRef = {
    val cluster = Cluster(system)
    if (cluster.getSelfRoles.contains(QuckooRoles.Registry)) {
      ClusterSharding(system).start(
        typeName = PersistentJob.ShardName,
        entityProps = PersistentJob.props,
        settings = ClusterShardingSettings(system).withRole(QuckooRoles.Registry),
        extractEntityId = PersistentJob.idExtractor,
        extractShardId = PersistentJob.shardResolver
      )
    } else {
      ClusterSharding(system).startProxy(
        typeName = PersistentJob.ShardName,
        role = Some(QuckooRoles.Registry),
        extractEntityId = PersistentJob.idExtractor,
        extractShardId = PersistentJob.shardResolver
      )
    }
  }

}

class Registry private (settings: RegistrySettings, journal: QuckooJournal)
    extends Actor with ActorLogging with Stash {
  import Registry._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system),
    "registry"
  )

  private[this] val mediator    = DistributedPubSub(context.system).mediator
  private[this] val resolver    = context.actorOf(settings.resolverProps, "resolver")
  private[this] val shardRegion = startShardRegion(context.system)

  private[this] var jobIds = Set.empty[JobId]

  override def preStart(): Unit = {
    mediator ! DistributedPubSubMediator.Subscribe(TopicTag.Registry.name, self)
  }

  override def postStop(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(TopicTag.Registry.name, self)
  }

  def receive: Receive = initializing

  private def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      warmUp()
      context become ready

    case _ => stash()
  }

  private def ready: Receive = {
    case WarmUp.Start =>
      log.info("Registry warm up started...")
      sender() ! WarmUp.Ack
      context become warmingUp

    case RegisterJob(spec) =>
      val registrationProps = Registration.props(spec, shardRegion, resolver, sender())
      context.actorOf(registrationProps, s"registration-${UUID.randomUUID()}")

    case GetJobs =>
      val origSender = sender()

      def fetchJobAsync(jobId: JobId): Future[(JobId, JobSpec)] = {
        import context.dispatcher

        implicit val timeout = Timeout(2 seconds)
        (shardRegion ? GetJob(jobId)).mapTo[JobSpec].map(jobId -> _)
      }

      Source(jobIds)
        .mapAsync(2)(fetchJobAsync)
        .runWith(Sink.actorRef(origSender, Status.Success(GetJobs)))

    case get @ GetJob(jobId) =>
      if (jobIds.contains(jobId)) {
        shardRegion forward get
      } else {
        sender() ! JobNotFound(jobId)
      }

    case msg: RegistryJobCommand =>
      if (jobIds.contains(msg.jobId)) shardRegion forward msg
      else sender() ! JobNotFound(msg.jobId)

    case event: RegistryEvent =>
      handleEvent(event)
  }

  private def warmingUp: Receive = {
    case EventEnvelope2(_, _, _, event: RegistryEvent) =>
      handleEvent(event)
      sender() ! WarmUp.Ack

    case WarmUp.Completed =>
      log.info("Registry warming up finished.")
      context.system.eventStream.publish(Ready)
      unstashAll()
      context become ready

    case WarmUp.Failed(ex) =>
      log.error("Error during Registry warm up: {}", ex.getMessage)
      import context.dispatcher
      context.system.scheduler.scheduleOnce(2 seconds, () => warmUp())
      unstashAll()
      context become ready

    case _: RegistryCommand => stash()
  }

  private def handleEvent(event: RegistryEvent): Unit = event match {
    case JobAccepted(jobId, _) =>
      log.debug("Indexing job {}", jobId)
      jobIds += jobId

    case _ =>
  }

  private def warmUp(): Unit = {
    journal.read
      .currentEventsByTag(EventTag, journal.firstOffset)
      .runWith(
        Sink.actorRefWithAck(self, WarmUp.Start, WarmUp.Ack, WarmUp.Completed, WarmUp.Failed))
  }

}
