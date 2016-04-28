package io.quckoo.cluster.scheduler

import akka.actor.{Actor, ActorRef, ActorLogging, Props, Stash, Status}
import akka.cluster.Cluster
import akka.cluster.ddata._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import io.quckoo.ExecutionPlan
import io.quckoo.id.PlanId
import io.quckoo.cluster.topics
import io.quckoo.protocol.scheduler._

object ExecutionPlanIndex {

  final val ExecutionPlanKey = ORSetKey[PlanId]("executionPlanIndex")

  final case class Query(cmd: SchedulerCommand, sender: ActorRef)

  sealed trait IndexOp
  case object AddToIndex extends IndexOp
  case object RemoveFromIndex extends IndexOp

  def props(shardRegion: ActorRef): Props =
    Props(classOf[ExecutionPlanIndex], shardRegion)

}

class ExecutionPlanIndex(shardRegion: ActorRef) extends Actor with ActorLogging with Stash {
  import ExecutionPlanIndex._
  import Replicator.{ReadLocal, WriteLocal}

  implicit val cluster = Cluster(context.system)
  private[this] val replicator = DistributedData(context.system).replicator
  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit = {
    log.debug("Starting Scheduler index...")
    context.system.eventStream.subscribe(self, classOf[SchedulerEvent])
  }

  def receive = ready

  def ready: Receive = {
    case event @ ExecutionPlanStarted(_, planId) =>
      log.debug("Indexing execution plan {}", planId)
      mediator ! DistributedPubSubMediator.Publish(topics.Scheduler, event)
      updateIndex(AddToIndex, planId)
      context become updatingIndex(planId)

    case event @ ExecutionPlanFinished(_, planId) =>
      log.debug("Removing execution plan {} from the index.", planId)
      mediator ! DistributedPubSubMediator.Publish(topics.Scheduler, event)
      updateIndex(RemoveFromIndex, planId)
      context become updatingIndex(planId)

    case cmd: GetExecutionPlan =>
      val externalReq = Query(cmd, sender())
      val replicatorReq = Replicator.Get(`ExecutionPlanKey`, ReadLocal, Some(externalReq))
      val handler = context.actorOf(Props(classOf[ExecutionPlanSingleQuery], shardRegion))
      replicator.tell(replicatorReq, handler)

    case GetExecutionPlans =>
      val externalReq = Query(GetExecutionPlans, sender())
      val replicatorReq = Replicator.Get(`ExecutionPlanKey`, ReadLocal, Some(externalReq))
      val handler = context.actorOf(Props(classOf[ExecutionPlanMultiQuery], shardRegion))
      replicator.tell(replicatorReq, handler)
  }

  def updatingIndex(planId: PlanId, attempts: Int = 1): Receive = {
    case Replicator.UpdateSuccess(`ExecutionPlanKey` , _) =>
      unstashAll()
      context become ready

    case Replicator.UpdateTimeout(`ExecutionPlanKey`, Some(op: IndexOp)) =>
      if (attempts < 3) {
        log.warning("Timed out while updating index for execution plan {}, in operation",
          planId, op)
        updateIndex(op, planId)
        context become updatingIndex(planId, attempts + 1)
      } else {
        log.error("Could not index execution plan {}", planId)
        unstashAll()
        context become ready
      }

    case _ => stash()
  }

  private[this] def updateIndex(op: IndexOp, planId: PlanId): Unit = op match {
    case AddToIndex =>
      replicator ! Replicator.Update(ExecutionPlanKey, ORSet.empty[PlanId],
        WriteLocal, Some(op))(_ + planId)

    case RemoveFromIndex =>
      replicator ! Replicator.Update(ExecutionPlanKey, ORSet.empty[PlanId],
        WriteLocal, Some(op))(_ - planId)
  }

}

private class ExecutionPlanSingleQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import ExecutionPlanIndex._

  def receive: Receive = {
    case res @ Replicator.GetSuccess(`ExecutionPlanKey`, Some(Query(cmd @ GetExecutionPlan(planId), requestor))) =>
      val elems = res.get(ExecutionPlanKey).elements
      if (elems.contains(planId)) {
        log.debug("Found execution plan {} in the index, retrieving its state", planId)
        shardRegion.tell(cmd, requestor)
      } else {
        log.debug("Execution plan {} not found in the index", planId)
        requestor ! ExecutionPlanNotFound(planId)
      }
      context stop self

    case Replicator.NotFound(`ExecutionPlanKey`, Some(Query(GetExecutionPlan(planId), requestor))) =>
      requestor ! ExecutionPlanNotFound(planId)
      context stop self

    case Replicator.GetFailure(`ExecutionPlanKey`, Some(Query(_, requestor))) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

}

private class ExecutionPlanMultiQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import ExecutionPlanIndex._

  private[this] var expectedResultCount = 0

  def receive = {
    case res @ Replicator.GetSuccess(`ExecutionPlanKey`, Some(Query(GetExecutionPlans, requestor))) =>
      val elems = res.get(ExecutionPlanKey).elements
      if (elems.nonEmpty) {
        expectedResultCount = elems.size
        log.debug("Found {} active execution plans", elems.size)
        elems.foreach { planId =>
          shardRegion ! GetExecutionPlan(planId)
        }
        context become collateResults(requestor)
      } else {
        log.debug("ExecutionPlan index is empty")
        completeQuery(requestor)
      }

    case Replicator.NotFound(`ExecutionPlanKey`, Some(Query(_, requestor))) =>
      // complete query normally
      completeQuery(requestor)

    case Replicator.GetFailure(`ExecutionPlanKey`, Some(Query(_, requestor))) =>
      requestor ! Status.Failure(new Exception("Could not retrieve elements from the index"))
      context stop self
  }

  private[this] def collateResults(requestor: ActorRef): Receive = {
    case plan: ExecutionPlan if expectedResultCount > 0 =>
      requestor ! plan
      expectedResultCount -= 1
      if (expectedResultCount == 0) {
        completeQuery(requestor)
      }
  }

  private[this] def completeQuery(requestor: ActorRef): Unit = {
    requestor ! Status.Success(())
    context stop self
  }

}
