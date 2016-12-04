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

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Stash, Status}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.persistence.query.{EventEnvelope2, Sequence}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import io.quckoo.JobSpec
import io.quckoo.id.JobId
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.journal.QuckooJournal
import io.quckoo.cluster.topics
import io.quckoo.fault._
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
    val resolve = IvyResolve(settings.resolver)
    val props   = Resolver.props(resolve).withDispatcher("quckoo.resolver.dispatcher")
    Props(classOf[Registry], RegistrySettings(props), journal)
  }

  def props(settings: RegistrySettings, journal: QuckooJournal): Props =
    Props(classOf[Registry], settings, journal)

}

class Registry(settings: RegistrySettings, journal: QuckooJournal)
    extends Actor with ActorLogging with Stash {
  import Registry._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system),
    "registry"
  )

  private[this] val cluster     = Cluster(context.system)
  private[this] val mediator    = DistributedPubSub(context.system).mediator
  private[this] val resolver    = context.actorOf(settings.resolverProps, "resolver")
  private[this] val shardRegion = startShardRegion

  private[this] var jobIds = Set.empty[JobId]

  private[this] var handlerRefCount = 0L

  override def preStart(): Unit = {
    mediator ! DistributedPubSubMediator.Subscribe(topics.Registry, self)
  }

  override def postStop(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Registry, self)
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
      handlerRefCount += 1
      val handler = context.actorOf(handlerProps(spec, sender()), s"handler-$handlerRefCount")
      resolver.tell(Resolver.Validate(spec.artifactId), handler)

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

  private def startShardRegion: ActorRef =
    if (cluster.selfRoles.contains("registry")) {
      log.info("Starting registry shards...")
      ClusterSharding(context.system).start(
        typeName = PersistentJob.ShardName,
        entityProps = PersistentJob.props,
        settings = ClusterShardingSettings(context.system).withRole("registry"),
        extractEntityId = PersistentJob.idExtractor,
        extractShardId = PersistentJob.shardResolver
      )
    } else {
      log.info("Starting registry proxy...")
      ClusterSharding(context.system).startProxy(
        typeName = PersistentJob.ShardName,
        role = None,
        extractEntityId = PersistentJob.idExtractor,
        extractShardId = PersistentJob.shardResolver
      )
    }

  private def warmUp(): Unit = {
    journal.read
      .currentEventsByTag(EventTag, Sequence(0))
      .runWith(
        Sink.actorRefWithAck(self, WarmUp.Start, WarmUp.Ack, WarmUp.Completed, WarmUp.Failed))
  }

  private def handlerProps(jobSpec: JobSpec, replyTo: ActorRef): Props =
    Props(classOf[RegistryResolutionHandler], jobSpec, shardRegion, replyTo)

}

private class RegistryResolutionHandler(jobSpec: JobSpec, shardRegion: ActorRef, replyTo: ActorRef)
    extends Actor with ActorLogging with Stash {
  import Resolver._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private val jobId          = JobId(jobSpec)

  override def preStart(): Unit = {
    mediator ! DistributedPubSubMediator.Subscribe(topics.Registry, self)
  }

  def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      unstashAll()
      context become resolvingArtifact

    case _ => stash()
  }

  def resolvingArtifact: Receive = {
    case ArtifactResolved(artifact) =>
      log.debug("Job artifact has been successfully resolved. artifactId={}", artifact.artifactId)
      shardRegion ! PersistentJob.CreateJob(jobId, jobSpec)
      context.setReceiveTimeout(10 seconds)
      context become registeringJob

    case ResolutionFailed(_, cause) =>
      log.error("Couldn't validate the job artifact id. " + cause)
      replyTo ! JobRejected(jobId, cause)
      finish()
  }

  def registeringJob: Receive = {
    case evt @ JobAccepted(`jobId`, _) =>
      replyTo ! evt
      finish()

    case ReceiveTimeout =>
      log.error("Timed out whilst storing the job details. jobId={}", jobId)
      replyTo ! JobRejected(jobId, ExceptionThrown.from(new TimeoutException))
      finish()
  }

  def stopping: Receive = {
    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context stop self
  }

  private[this] def finish(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Registry, self)
    context become stopping
  }

}
