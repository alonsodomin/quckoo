package io.quckoo.cluster.registry

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, Status}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{ReadLocal, WriteLocal}
import akka.cluster.ddata._
import akka.persistence.query.EventEnvelope
import akka.stream.actor._
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import io.quckoo.cluster.core.QuckooJournal
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._

/**
  * Created by alonsodomin on 13/04/2016.
  */
object RegistryPartitionIndex {

  final val IndexKey = ORSetKey[JobId]("registryIndex")

  final val DefaultHighWatermark = 100

  case object GetJobIds

  final case class IndexPartition(partitionId: String)
  final case class PartitionFinished(partitionId: String)

  def props(highWatermark: Int = DefaultHighWatermark): Props =
    Props(classOf[RegistryPartitionIndex], highWatermark)

}

class RegistryPartitionIndex(highWatermark: Int)
    extends ActorSubscriber with ActorLogging with QuckooJournal {
  import ActorSubscriberMessage._
  import RegistryPartitionIndex._

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private[this] var indexedPartitions = Set.empty[String]

  override def preStart(): Unit =
    context.system.eventStream.subscribe(self, classOf[IndexPartition])

  override def actorSystem = context.system

  override def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override def receive: Receive = {
    case IndexPartition(partitionId) if !indexedPartitions.contains(partitionId) =>
      log.debug("Indexing registry partition: {}", partitionId)
      indexedPartitions += partitionId
      readJournal.eventsByPersistenceId(partitionId, -1, Long.MaxValue).
        runWith(Sink.actorRef(self, PartitionFinished(partitionId)))

    case EventEnvelope(_, _, _, event) =>
      event match {
        case JobAccepted(jobId, _) =>
          log.debug("Indexing job {}", jobId)
          replicator ! Replicator.Update(IndexKey, ORSet.empty[JobId], WriteLocal)(_ + jobId)

        case _ => // do nothing
      }

    case Status.Failure(ex) =>
      log.error(ex, "Error indexing partitions.")

    case GetJobIds =>
      log.debug("Grabbing all job ids from the index")
      replicator ! Replicator.Get(IndexKey, ReadLocal, Some(sender()))

    case r @ Replicator.GetSuccess(`IndexKey`, Some(requestor: ActorRef)) =>
      val elems = r.get(IndexKey).elements
      log.debug("Found {} elements in the index", elems.size)
      elems.foreach { jobId =>
        requestor ! jobId
      }
      requestor ! Status.Success(())

    case Replicator.NotFound(`IndexKey`, Some(requestor: ActorRef)) =>
      requestor ! Status.Success(())

    case Replicator.GetFailure(`IndexKey`, Some(requestor: ActorRef)) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))

    case PartitionFinished(partitionId) =>
      indexedPartitions -= partitionId
      if (indexedPartitions.isEmpty)
        context stop self
  }

}
