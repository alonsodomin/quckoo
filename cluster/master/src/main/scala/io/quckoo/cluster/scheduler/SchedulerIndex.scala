package io.quckoo.cluster.scheduler

import akka.actor.{Actor, ActorRef, ActorLogging, Props, Stash, Status}
import akka.cluster.Cluster
import akka.cluster.ddata._

import io.quckoo.id.PlanId
import io.quckoo.protocol.scheduler._

object SchedulerIndex {

  final val ExecutionPlanKey = ORSetKey[PlanId]("executionPlanIndex")

  final case class IndexExecutionPlan(plan: PlanId)
  final case class Query(cmd: SchedulerCommand, sender: ActorRef)

  def props(shardRegion: ActorRef): Props =
    Props(classOf[SchedulerIndex], shardRegion)

}

class SchedulerIndex(shardRegion: ActorRef) extends Actor with ActorLogging with Stash {
  import SchedulerIndex._
  import Replicator.{ReadLocal, WriteLocal}

  implicit val cluster = Cluster(context.system)
  val replicator = DistributedData(context.system).replicator

  override def preStart(): Unit = {
    log.debug("Starting Scheduler index...")
    context.system.eventStream.subscribe(self, classOf[IndexExecutionPlan])
  }

  def receive = ready

  def ready: Receive = {
    case IndexExecutionPlan(planId) =>
      log.debug("Indexing execution plan {}", planId)
      addPlanToIndex(planId)
      context become updatingIndex(planId)

    case cmd: GetExecutionPlan =>
      val externalReq = Query(cmd, sender())
      val replicatorReq = Replicator.Get(`ExecutionPlanKey`, ReadLocal, Some(externalReq))
      val handler = context.actorOf(Props(classOf[SchedulerSingleQuery], shardRegion))
      replicator.tell(replicatorReq, handler)
  }

  def updatingIndex(planId: PlanId, attempts: Int = 1): Receive = {
    case Replicator.UpdateSuccess(`ExecutionPlanKey` , _) =>
      unstashAll()
      context become ready

    case Replicator.UpdateTimeout(`ExecutionPlanKey`, _) =>
      if (attempts < 3) {
        log.warning("Timed out while indexing execution plan {}", planId)
        addPlanToIndex(planId)
        context become updatingIndex(planId, attempts + 1)
      } else {
        log.error("Could not index execution plan {}", planId)
        unstashAll()
        context become ready
      }

    case _ => stash()
  }

  private[this] def addPlanToIndex(planId: PlanId): Unit =
    replicator ! Replicator.Update(ExecutionPlanKey, ORSet.empty[PlanId], WriteLocal)(_ + planId)

}

private class SchedulerSingleQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import SchedulerIndex._

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

private class SchedulerMultiQuery(shardRegion: ActorRef) extends Actor with ActorLogging {
  import SchedulerIndex._

  def receive = {
    case res @ Replicator.GetSuccess(`ExecutionPlanKey`, Some(Query(GetExecutionPlans, requestor))) =>
      val elems = res.get(ExecutionPlanKey).elements

  }

  private[this] def completeQuery(requestor: ActorRef): Unit = {
    requestor ! Status.Success(())
    context stop self
  }

}
