package io.quckoo.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Status}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{ReadLocal, WriteLocal}
import akka.cluster.ddata._
import akka.persistence.query.EventEnvelope
import akka.stream.actor._
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import io.quckoo.JobSpec
import io.quckoo.cluster.core.QuckooJournal
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._

/**
  * Created by alonsodomin on 13/04/2016.
  */
object RegistryIndex {

  final val IndexKey = ORSetKey[JobId]("registryIndex")

  final val DefaultHighWatermark = 100

  final case class Fetch(request: RegistryReadCommand, sender: ActorRef)
  final case class CollateResults(size: Int)

  final case class IndexJob(persistenceId: String)
  final case class PartitionFinished(persistenceId: String)

  def props(shardRegion: ActorRef, highWatermark: Int = DefaultHighWatermark): Props =
    Props(classOf[RegistryIndex], shardRegion, highWatermark)

}

class RegistryIndex(shardRegion: ActorRef, highWatermark: Int)
    extends ActorSubscriber with ActorLogging with QuckooJournal {
  import RegistryIndex._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private[this] var indexedPersistenceIds = Set.empty[String]

  override def preStart(): Unit =
    context.system.eventStream.subscribe(self, classOf[IndexJob])

  override def actorSystem = context.system

  override def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override def receive: Receive = {
    case IndexJob(persistenceId) if !indexedPersistenceIds.contains(persistenceId) =>
      log.debug("Indexing registry partition: {}", persistenceId)
      indexedPersistenceIds += persistenceId
      readJournal.eventsByPersistenceId(persistenceId, -1, Long.MaxValue).
        runWith(Sink.actorRef(self, PartitionFinished(persistenceId)))

    case EventEnvelope(_, _, _, event) =>
      event match {
        case JobAccepted(jobId, _) =>
          log.debug("Indexing job {}", jobId)
          replicator ! Replicator.Update(IndexKey, ORSet.empty[JobId], WriteLocal)(_ + jobId)

        case _ => // do nothing
      }

    case Status.Failure(ex) =>
      log.error(ex, "Error indexing partitions.")

    case GetJobs =>
      log.debug("Grabbing all job ids from the index")
      val externalReq = Fetch(GetJobs, sender())
      val replicatorReq = Replicator.Get(IndexKey, ReadLocal, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistryQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)

    case msg: GetJob =>
      replicator ! Replicator.Get(IndexKey, ReadLocal, Some(Fetch(msg, sender())))

    case r @ Replicator.GetSuccess(`IndexKey`, Some(Fetch(cmd: GetJob, requestor))) =>
      val elems = r.get(IndexKey).elements
      if (elems.contains(cmd.jobId)) {
        shardRegion.tell(cmd, requestor)
      } else {
        requestor ! JobNotFound(cmd.jobId)
      }

    case Replicator.NotFound(`IndexKey`, Some(Fetch(GetJob(jobId), requestor))) =>
      requestor ! JobNotFound(jobId)

    case Replicator.GetFailure(`IndexKey`, Some(Fetch(_, requestor: ActorRef))) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))

    case PartitionFinished(partitionId) =>
      indexedPersistenceIds -= partitionId
      if (indexedPersistenceIds.isEmpty)
        context stop self
  }

}

private class RegistryQuery(shardRegion: ActorRef) extends Actor {
  import RegistryIndex._

  private[this] var expectedResultCount = 0

  def receive = {
    case res @ Replicator.GetSuccess(`IndexKey`, Some(req @ Fetch(GetJobs, requestor))) =>
      val elems = res.get(IndexKey).elements
      if (elems.nonEmpty) {
        expectedResultCount = elems.size
        elems.foreach { jobId =>
          shardRegion ! GetJob(jobId)
        }
        context become collateResults(requestor)
      } else {
        completeQuery(requestor)
      }

    case Replicator.NotFound(`IndexKey`, Some(Fetch(_, requestor))) =>
      completeQuery(requestor)

    case Replicator.GetFailure(`IndexKey`, Some(Fetch(_, requestor: ActorRef))) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

  def collateResults(requestor: ActorRef): Receive = {
    case JobNotFound(_) =>
      // ignore "not found responses when collating query results"
      decreaseCountAndComplete(requestor)

    case (jobId: JobId, spec: JobSpec) =>
      requestor ! (jobId -> spec)
      decreaseCountAndComplete(requestor)

  }

  def decreaseCountAndComplete(requestor: ActorRef): Unit = {
    expectedResultCount -= 1
    if (expectedResultCount == 0) {
      completeQuery(requestor)
    }
  }

  def completeQuery(requestor: ActorRef): Unit = {
    requestor ! Status.Success(())
    context stop self
  }

}
