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
import io.quckoo.Task
import io.quckoo.id._
import io.quckoo.protocol.scheduler.{GetTasks, TaskDetails}

object ExecutionIndex {

  final val ExecutionIndexKey = ORSetKey[TaskId]("executionIndex")

  type Journal = AllPersistenceIdsQuery with EventsByPersistenceIdQuery

  def props(journal: Journal): Props = Props(classOf[ExecutionIndex], journal)

}

class ExecutionIndex(journal: ExecutionIndex.Journal) extends ActorSubscriber with ActorLogging {

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system), "executionIndex")

  private[this] var tasks = Map.empty[TaskId, TaskDetails]
  private[this] var tasksByPlan = Map.empty[PlanId, Set[TaskId]].withDefaultValue(Set.empty[TaskId])

  override def preStart(): Unit = {
    log.info("Starting Execution index...")
    journal.allPersistenceIds().
      filter(_.startsWith(Execution.PersistenceIdPrefix)).
      flatMapConcat(persistenceId => journal.eventsByPersistenceId(persistenceId, 0, Long.MaxValue)).
      runWith(Sink.actorRef(self, PoisonPill))
  }

  protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  def receive: Receive = {
    case GetTasks =>
      log.debug("Retrieving tasks from the index...")
      sender() ! tasks

    case EventEnvelope(offset, persistenceId, sequenceNr, event) =>
      log.debug(s"Received event: $event")
      event match {
        case Execution.Awaken(task, planId, _) =>
          tasks += task.id -> TaskDetails(task.artifactId, task.jobClass, Task.NotStarted)
          tasksByPlan += planId -> (tasksByPlan(planId) + task.id)

        case _ =>
      }
  }
}
