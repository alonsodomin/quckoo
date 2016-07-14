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

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ddata._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.{CurrentEventsByTagQuery, EventsByTagQuery}
import akka.stream.actor._
import akka.stream.scaladsl.Sink
import io.quckoo.JobSpec
import io.quckoo.id.JobId
import io.quckoo.cluster.topics
import io.quckoo.protocol.registry._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 13/04/2016.
  */
object RegistryIndex {

  final val DefaultTimeout = 5 seconds

  final val JobIdsKey = GSetKey[JobId]("jobIds")

  final case class Query(request: RegistryReadCommand, replyTo: ActorRef)
  final case class UpdateIndex(event: RegistryEvent, replyTo: Option[ActorRef] = None)

  case object Ack
  case object WarmUpStarted
  case object WarmedUp

  final case class IndexTimeoutException(attempts: Int)
    extends Exception(s"Index operation timed out after $attempts attempts.")

  def props(shardRegion: ActorRef, timeout: FiniteDuration = DefaultTimeout): Props =
    Props(classOf[RegistryIndex], shardRegion, timeout)

}

class RegistryIndex(shardRegion: ActorRef, journal: CurrentEventsByTagQuery, timeout: FiniteDuration)
    extends ActorSubscriber with ActorLogging with Stash {
  import RegistryIndex._
  import Replicator._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator
  private[this] val mediator = DistributedPubSub(context.system).mediator

  log.info("Starting registry index...")

  override protected val requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override protected def preStart(): Unit = readFromJournal()

  override def receive = ready

  def warmingUp: Receive = {
    case EventEnvelope(offset, _, _, event @ JobAccepted(jobId, _)) =>
      log.debug("Indexing job {}", jobId)
      val req = UpdateIndex(event, Some(sender()))
      if (replicatorUpdate.isDefinedAt(req)) {
        replicator ! replicatorUpdate(req)
        context become updatingIndex()
      } else {
        sender() ! Ack
      }

    case WarmedUp =>
      log.info("Registry index warming up finished.")
      unstashAll()
      context become ready

    case _: RegistryReadCommand => stash()
  }

  def ready: Receive = {
    case WarmUpStarted =>
      log.info("Index warm up starting...")
      sender() ! Ack
      context become warmingUp

    case event: RegistryEvent =>
      val req = UpdateIndex(event)

    case GetJobs =>
      val externalReq = Query(GetJobs, sender())
      val replicatorReq = Get(JobIdsKey, readConsistency, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistryMultiQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)

    case msg: GetJob =>
      val externalReq = Query(msg, sender())
      val replicatorReq = Get(JobIdsKey, readConsistency, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistrySingleQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)
  }

  private[this] def updatingIndex(attempt: Int = 1): Receive = {
    case UpdateSuccess(JobIdsKey, Some(UpdateIndex(_, replyTo))) =>
      replyTo.foreach(_ ! Ack)
      unstashAll()
      context unbecome

    case UpdateTimeout(JobIdsKey, Some(req @ UpdateIndex(event, replyTo))) =>
      if (attempt < 3) {
        log.warning("Timed out when performing index update for event {}. Retrying...", event)
        if (replicatorUpdate.isDefinedAt(req)) {
          replicator ! replicatorUpdate(req)
          context become updatingIndex(attempt + 1)
        }
      } else {
        log.error("Could not perform index update for event: {}", event)
        replyTo.foreach(_ ! Status.Failure(IndexTimeoutException(attempt)))
        unstashAll()
        context unbecome
      }

    case ModifyFailure(JobIdsKey, errorMessage, cause, Some(UpdateIndex(_, replyTo))) =>
      log.error(cause, errorMessage)
      replyTo.foreach(_ ! Status.Failure(cause))
      unstashAll()
      context unbecome

    case _ => stash()
  }

  private[this] def readFromJournal(offset: Long = 0): Unit = {
    journal.currentEventsByTag(Registry.EventTag, offset).
      runWith(Sink.actorRefWithAck(self, WarmUpStarted, Ack, WarmedUp))
  }

  private[this] def replicatorUpdate: PartialFunction[UpdateIndex, Update[GSet[JobId]]] = {
    case req @ UpdateIndex(JobAccepted(jobId, _), _) =>
      Update(JobIdsKey, GSet.empty[JobId], writeConsistency, Some(req))(_ + jobId)
  }

  private[this] def registryMembers =
    cluster.state.members.filter(_.roles.contains("registry"))

  private[this] def readConsistency = {
    if (registryMembers.size > 1) {
      ReadMajority(timeout)
    } else {
      ReadLocal
    }
  }

  private[this] def writeConsistency = {
    if (registryMembers.size > 1) {
      WriteMajority(timeout)
    } else {
      WriteLocal
    }
  }

}

private class RegistrySingleQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import RegistryIndex._
  import Replicator._

  def receive = {
    case r @ GetSuccess(JobIdsKey, Some(Query(cmd: GetJob, replyTo))) =>
      val elems = r.get(JobIdsKey).elements
      if (elems.contains(cmd.jobId)) {
        log.debug("Found job {} in the registry index, retrieving its state...", cmd.jobId)
        shardRegion.tell(cmd, replyTo)
      } else {
        log.info("Job {} was not found in the registry.", cmd.jobId)
        replyTo ! JobNotFound(cmd.jobId)
      }
      context stop self

    case NotFound(JobIdsKey, Some(Query(GetJob(jobId), replyTo))) =>
      replyTo ! JobNotFound(jobId)
      context stop self

    case GetFailure(JobIdsKey, Some(Query(_, replyTo))) =>
      replyTo ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

}

private class RegistryMultiQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import RegistryIndex._
  import Replicator._

  private[this] var expectedResultCount = 0

  def receive = {
    case res @ GetSuccess(JobIdsKey, Some(req @ Query(GetJobs, replyTo))) =>
      val elems = res.get(JobIdsKey).elements
      if (elems.nonEmpty) {
        expectedResultCount = elems.size
        log.debug("Found {} jobs currently in the index", expectedResultCount)
        elems.foreach { jobId =>
          shardRegion ! GetJob(jobId)
        }
        context become collateResults(replyTo)
      } else {
        log.debug("Job index is empty.")
        completeQuery(replyTo)
      }

    case NotFound(JobIdsKey, Some(Query(_, replyTo))) =>
      completeQuery(replyTo)

    case GetFailure(JobIdsKey, Some(Query(_, replyTo: ActorRef))) =>
      replyTo ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

  def collateResults(requestor: ActorRef): Receive = {
    case JobNotFound(_) if expectedResultCount > 0 =>
      // ignore "not found responses when collating query results"
      decreaseCountAndComplete(requestor)

    case (jobId: JobId, spec: JobSpec) if expectedResultCount > 0 =>
      requestor ! (jobId -> spec)
      decreaseCountAndComplete(requestor)

  }

  private def decreaseCountAndComplete(requestor: ActorRef): Unit = {
    expectedResultCount -= 1
    if (expectedResultCount == 0) {
      completeQuery(requestor)
    }
  }

  private def completeQuery(requestor: ActorRef): Unit = {
    requestor ! Status.Success(())
    context stop self
  }

}
