package io.quckoo.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
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

  final case class Query(request: RegistryReadCommand, sender: ActorRef)

  final case class IndexJob(persistenceId: String)
  final case class JobDisposed(persistenceId: String)

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

  override def preStart(): Unit = {
    log.info("Starting registry index...")
    context.system.eventStream.subscribe(self, classOf[IndexJob])
  }

  override def postStop(): Unit =
    context.system.eventStream.unsubscribe(self)

  override def actorSystem = context.system

  override def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override def receive: Receive = {
    case IndexJob(persistenceId) if !indexedPersistenceIds.contains(persistenceId) =>
      log.debug("Indexing job {}", persistenceId)
      indexedPersistenceIds += persistenceId
      readJournal.eventsByPersistenceId(persistenceId, -1, Long.MaxValue).
        runWith(Sink.actorRef(self, JobDisposed(persistenceId)))

    case EventEnvelope(_, _, _, event) =>
      event match {
        case JobAccepted(jobId, _) =>
          log.debug("Indexing job {}", jobId)
          replicator ! Replicator.Update(IndexKey, ORSet.empty[JobId], WriteLocal)(_ + jobId)

        case _ => // do nothing
      }

    case Status.Failure(ex) =>
      log.error(ex, "Error indexing jobs.")
      context stop self

    case GetJobs =>
      val externalReq = Query(GetJobs, sender())
      val replicatorReq = Replicator.Get(IndexKey, ReadLocal, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistryMultiQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)

    case msg: GetJob =>
      val externalReq = Query(msg, sender())
      val replicatorReq = Replicator.Get(IndexKey, ReadLocal, Some(externalReq))
      val queryHandler = context.actorOf(Props(classOf[RegistrySingleQuery], shardRegion))
      replicator.tell(replicatorReq, queryHandler)

    case JobDisposed(partitionId) =>
      indexedPersistenceIds -= partitionId
      if (indexedPersistenceIds.isEmpty)
        context stop self
  }

}

private class RegistrySingleQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import RegistryIndex._

  def receive = {
    case r @ Replicator.GetSuccess(`IndexKey`, Some(Query(cmd: GetJob, requestor))) =>
      val elems = r.get(IndexKey).elements
      if (elems.contains(cmd.jobId)) {
        log.debug("Found job {} in the registry index, retrieving its state...", cmd.jobId)
        shardRegion.tell(cmd, requestor)
      } else {
        log.info("Job {} was not found in the registry.", cmd.jobId)
        requestor ! JobNotFound(cmd.jobId)
      }
      context stop self

    case Replicator.NotFound(`IndexKey`, Some(Query(GetJob(jobId), requestor))) =>
      requestor ! JobNotFound(jobId)
      context stop self

    case Replicator.GetFailure(`IndexKey`, Some(Query(_, requestor: ActorRef))) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

}

private class RegistryMultiQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import RegistryIndex._

  private[this] var expectedResultCount = 0

  def receive = {
    case res @ Replicator.GetSuccess(`IndexKey`, Some(req @ Query(GetJobs, requestor))) =>
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

    case Replicator.NotFound(`IndexKey`, Some(Query(_, requestor))) =>
      completeQuery(requestor)

    case Replicator.GetFailure(`IndexKey`, Some(Query(_, requestor: ActorRef))) =>
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
