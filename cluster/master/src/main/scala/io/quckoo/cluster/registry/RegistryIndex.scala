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
import akka.stream.actor._

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

  final val IndexKey = GSetKey[JobId]("registryIndex")

  final case class Query(request: RegistryReadCommand, sender: ActorRef)
  final case class IndexJob(jobId: JobId)

  def props(shardRegion: ActorRef, timeout: FiniteDuration = DefaultTimeout): Props =
    Props(classOf[RegistryIndex], shardRegion, timeout)

}

class RegistryIndex(shardRegion: ActorRef, timeout: FiniteDuration) extends ActorSubscriber with ActorLogging with Stash {
  import RegistryIndex._
  import Replicator._
  import ActorSubscriberMessage._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator
  private[this] val mediator = DistributedPubSub(context.system).mediator

  log.info("Starting registry index...")

  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override def receive = ready

  def ready: Receive = {
    case OnNext(EventEnvelope(_, _, _, event @ JobAccepted(jobId, _))) =>
      log.debug("Indexing job {}", jobId)
      addJobIdToIndex(jobId)
      mediator ! DistributedPubSubMediator.Publish(topics.Registry, event)
      context become updatingIndex(jobId)

    case GetJobs =>
      val externalReq = Query(GetJobs, sender())
      val replicatorReq = Get(IndexKey, readConsistency, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistryMultiQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)

    case msg: GetJob =>
      val externalReq = Query(msg, sender())
      val replicatorReq = Get(IndexKey, readConsistency, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistrySingleQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)
  }

  def updatingIndex(jobId: JobId, attempts: Int = 1): Receive = {
    case UpdateSuccess(`IndexKey`, _) =>
      unstashAll()
      context become ready

    case UpdateTimeout(`IndexKey`, _) =>
      if (attempts < 3) {
        log.warning("Timed out when indexing job {}. Retrying...", jobId)
        addJobIdToIndex(jobId)
        context become updatingIndex(jobId, attempts + 1)
      } else {
        log.error("Could not add job {} to the index.", jobId)
        unstashAll()
        context become ready
      }

    case _ => stash()
  }

  private[this] def addJobIdToIndex(jobId: JobId): Unit = {
    replicator ! Update(IndexKey, GSet.empty[JobId], writeConsistency)(_ + jobId)
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
    case r @ GetSuccess(`IndexKey`, Some(Query(cmd: GetJob, replyTo))) =>
      val elems = r.get(IndexKey).elements
      if (elems.contains(cmd.jobId)) {
        log.debug("Found job {} in the registry index, retrieving its state...", cmd.jobId)
        shardRegion.tell(cmd, replyTo)
      } else {
        log.info("Job {} was not found in the registry.", cmd.jobId)
        replyTo ! JobNotFound(cmd.jobId)
      }
      context stop self

    case NotFound(`IndexKey`, Some(Query(GetJob(jobId), replyTo))) =>
      replyTo ! JobNotFound(jobId)
      context stop self

    case GetFailure(`IndexKey`, Some(Query(_, replyTo))) =>
      replyTo ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

}

private class RegistryMultiQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import RegistryIndex._
  import Replicator._

  private[this] var expectedResultCount = 0

  def receive = {
    case res @ GetSuccess(`IndexKey`, Some(req @ Query(GetJobs, requestor))) =>
      val elems = res.get(IndexKey).elements
      if (elems.nonEmpty) {
        expectedResultCount = elems.size
        log.debug("Found {} jobs currently in the index", expectedResultCount)
        elems.foreach { jobId =>
          shardRegion ! GetJob(jobId)
        }
        context become collateResults(requestor)
      } else {
        log.debug("Job index is empty.")
        completeQuery(requestor)
      }

    case NotFound(`IndexKey`, Some(Query(_, requestor))) =>
      completeQuery(requestor)

    case GetFailure(`IndexKey`, Some(Query(_, requestor: ActorRef))) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))
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
