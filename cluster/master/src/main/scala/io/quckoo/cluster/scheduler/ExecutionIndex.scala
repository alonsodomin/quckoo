package io.quckoo.cluster.scheduler

import akka.actor.{ActorLogging, PoisonPill, Props}
import akka.cluster.ddata._
import akka.persistence.fsm.PersistentFSM.StateChangeEvent
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.{AllPersistenceIdsQuery, EventsByPersistenceIdQuery}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import akka.stream.scaladsl.Sink
import io.quckoo.id._

object ExecutionIndex {

  final val ExecutionIndexKey = ORSetKey[TaskId]("executionIndex")

  type Journal = AllPersistenceIdsQuery with EventsByPersistenceIdQuery

  def props(journal: Journal): Props = Props(classOf[ExecutionIndex], journal)

}

class ExecutionIndex(journal: ExecutionIndex.Journal) extends ActorSubscriber with ActorLogging {

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system), "executionIndex")

  override def preStart(): Unit = {
    log.info("Starting Execution index...")
    journal.allPersistenceIds().
      filter(_.startsWith(Execution.PersistenceIdPrefix)).
      flatMapConcat(persistenceId => journal.eventsByPersistenceId(persistenceId, 0, Long.MaxValue)).
      runWith(Sink.actorRef(self, PoisonPill))
  }

  protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  def receive: Receive = {
    case EventEnvelope(offset, persistenceId, sequenceNr, event) =>
      log.debug(s"Received event: $event")
  }
}
