package io.chronos.scheduler.butler

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.contrib.pattern.{ClusterReceptionistExtension, DistributedPubSubExtension, DistributedPubSubMediator}
import akka.persistence.PersistentActor
import io.chronos.scheduler.protocol.WorkerProtocol
import io.chronos.scheduler.worker.{ExecutionPlan, Work, WorkResult}

import scala.concurrent.duration.{Deadline, FiniteDuration}

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object Butler {

  val ResultsTopic = "results"

  val Path = "/user/butler/active"

  def props(workTimeout: FiniteDuration): Props = Props(classOf[Butler], workTimeout)

  case class Ack(workId: String)

  private sealed trait WorkerStatus
  private case object Idle extends WorkerStatus
  private case class Busy(workId: String, deadline: Deadline) extends WorkerStatus

  private case class WorkerState(ref: ActorRef, status: WorkerStatus)

  private case object CleanupTick
}


class Butler(workTimeout: FiniteDuration) extends PersistentActor with ActorLogging {
  import Butler._
  import ExecutionPlan._
  import WorkerProtocol._

  val mediator = DistributedPubSubExtension(context.system).mediator
  ClusterReceptionistExtension(context.system).registerService(self)

  // persistenceId must include cluster role to support multiple masters
  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-master"
    case _ => "master"
  }

  // worker state is not event sourced
  private var workers = Map[String, WorkerState]()

  // work state is event sourced
  private var executionPlan = ExecutionPlan.empty

  import context.dispatcher
  val cleanupTask = context.system.scheduler.schedule(workTimeout / 2, workTimeout / 2, self, CleanupTick)
  override def postStop(): Unit = cleanupTask.cancel()

  def notifyWorkers(): Unit =
    if (executionPlan.hasWork) {
      workers.foreach {
        case (_, WorkerState(ref, Idle)) => ref ! WorkReady
        case _ => // busy
      }
    }

  def changeWorkerToIdle(workerId: String, workId: String): Unit =
    workers.get(workerId) match {
      case Some(s @ WorkerState(_, Busy(`workId`, _))) =>
        workers += (workerId -> s.copy(status = Idle))
      case _ => // might happen after standby recovery, worker state is not persisted
    }

  override def receiveRecover: Receive = {
    case event: WorkDomainEvent =>
      executionPlan = executionPlan.updated(event)
      log.info("Replayed {}", event.getClass.getSimpleName)
  }

  override def receiveCommand: Receive = {
    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = Idle))
        log.info("Worker registered: {}", workerId)
        if (executionPlan.hasWork) {
          sender() ! WorkReady
        }
      }

    case RequestWork(workerId) =>
      if (executionPlan.hasWork) {
        workers.get(workerId) match {
          case Some(workerState @ WorkerState(_, Idle)) =>
            val work = executionPlan.nextWork
            persist(WorkStarted(work.workId)) { event =>
              executionPlan = executionPlan.updated(event)
              log.info("Giving worker {} some work {}", workerId, work.workId)
              workers += (workerId -> workerState.copy(status = Busy(work.workId, Deadline.now + workTimeout)))
              sender() ! work
            }
          case _ =>
        }
      }

    case WorkDone(workerId, workId, result) =>
      if (executionPlan.isDone(workId)) {
        // previous Ack was lost, confirm again that this is done
        sender() ! WorkAck(workId)
      } else if (!executionPlan.isInProgress(workId)) {
        log.info("Work {} not in progress, reported as done by worker {}", workId, workerId)
      } else {
        log.info("Work {} is done by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        persist(WorkCompleted(workId, result)) { event =>
          executionPlan = executionPlan.updated(event)
          mediator ! DistributedPubSubMediator.Publish(ResultsTopic, WorkResult(workId, result))
          sender ! WorkAck(workId)
        }
      }

    case WorkFailed(workerId, workId) =>
      if (executionPlan.isInProgress(workId)) {
        log.info("Work {} failed by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        persist(WorkerFailed(workId)) { event =>
          executionPlan = executionPlan.updated(event)
          notifyWorkers()
        }
      }

    case work: Work =>
      if (executionPlan.isAccepted(work.workId)) {
        sender() ! Ack(work.workId)
      } else {
        log.info("Accepted work {}", work.workId)
        persist(WorkAccepted(work)) { event =>
          sender() ! Ack(work.workId)
          executionPlan = executionPlan.updated(event)
          notifyWorkers()
        }
      }

    case CleanupTick =>
      for ((workerId, workerState @ WorkerState(_, Busy(workId, timeout))) <- workers) {
        if (timeout.isOverdue()) {
          log.info("Work timed out: {}", workId)
          workers -= workerId
          persist(WorkerTimedOut(workId)) { event =>
            executionPlan = executionPlan.updated(event)
            notifyWorkers()
          }
        }
      }
  }
}
