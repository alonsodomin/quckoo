package io.chronos.scheduler

import java.time.Clock

import akka.actor.{ActorLogging, Props}
import akka.cluster.Cluster
import akka.contrib.pattern.{ClusterReceptionistExtension, DistributedPubSubExtension, DistributedPubSubMediator}
import akka.persistence.PersistentActor
import io.chronos.scheduler.id._
import io.chronos.scheduler.jobstore.JobStore
import io.chronos.scheduler.protocol.WorkerProtocol
import io.chronos.scheduler.worker.{ExecutionPlan, WorkResult, WorkerState}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Scheduler {

  val ResultsTopic = "results"

  val Path = "/user/scheduler/active"

  val defaultHeartbeatInterval = 1000.millis
  val defaultBatchSize = 10

  def props(clock: Clock,
            workTimeout: FiniteDuration,
            heartbeatInterval: FiniteDuration = defaultHeartbeatInterval,
            jobBatchSize: Int = defaultBatchSize): Props =
    Props(classOf[Scheduler], clock, workTimeout, heartbeatInterval, jobBatchSize)

  case class ScheduleJob(jobDefinition: JobDefinition)
  case class ScheduleAck(jobId: JobId)

  private case object Heartbeat
  private case object CleanupTick
}


class Scheduler(clock: Clock, workTimeout: FiniteDuration, heartbeatInterval: FiniteDuration, jobBatchSize: Int)
  extends PersistentActor with ActorLogging {

  import ExecutionPlan._
  import Scheduler._
  import WorkerProtocol._
  import context.dispatcher

  val mediator = DistributedPubSubExtension(context.system).mediator
  ClusterReceptionistExtension(context.system).registerService(self)

  // persistenceId must include cluster role to support multiple masters
  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-master"
    case _ => "master"
  }

  private val jobStore = new JobStore

  // worker state is not event sourced
  private var workers = Map[WorkerId, WorkerState]()

  // execution plan is event sourced
  private var executionPlan = ExecutionPlan.empty

  val cleanupTask = context.system.scheduler.schedule(workTimeout / 2, workTimeout / 2, self, CleanupTick)
  val heartbeatTask = context.system.scheduler.schedule(0.seconds, heartbeatInterval, self, Heartbeat)

  override def postStop(): Unit = {
    cleanupTask.cancel()
    heartbeatTask.cancel()
  }

  def notifyWorkers(): Unit =
    if (executionPlan.hasWork) {
      workers.foreach {
        case (_, WorkerState(ref, WorkerState.Idle)) => ref ! WorkReady
        case _ => // busy
      }
    }

  def changeWorkerToIdle(workerId: WorkerId, workId: WorkId): Unit =
    workers.get(workerId) match {
      case Some(workerState @ WorkerState(_, WorkerState.Busy(`workId`, _))) =>
        workers += (workerId -> workerState.copy(status = WorkerState.Idle))
      case _ => // might happen after standby recovery, worker state is not persisted
    }

  override def receiveRecover: Receive = {
    case event: WorkDomainEvent =>
      executionPlan = executionPlan.updated(event)
      log.info("Replayed {}", event.getClass.getSimpleName)
  }

  override def receiveCommand: Receive = {
    case ScheduleJob(jobDef) =>
      log.info("Job scheduled. jobId={}", jobDef.jobId)
      jobStore.push(jobDef)
      sender() ! ScheduleAck(jobDef.jobId)

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered: {}", workerId)
        if (executionPlan.hasWork) {
          sender() ! WorkReady
        }
      }

    case RequestWork(workerId) =>
      if (executionPlan.hasWork) {
        workers.get(workerId) match {
          case Some(workerState @ WorkerState(_, WorkerState.Idle)) =>
            val work = executionPlan.nextWork
            persist(WorkStarted(work.id)) { event =>
              executionPlan = executionPlan.updated(event)
              log.info("Giving worker {} some work {}", workerId, work.id)
              workers += (workerId -> workerState.copy(status = WorkerState.Busy(work.id, Deadline.now + workTimeout)))
              sender() ! work
            }
          case _ =>
        }
      }

    case WorkDone(workerId, workId, result) =>
      if (executionPlan.isDone(workId)) {
        // previous Ack was lost, confirm again that this is done
        sender() ! WorkDoneAck(workId)
      } else if (!executionPlan.isInProgress(workId)) {
        log.info("Work {} not in progress, reported as done by worker {}", workId, workerId)
      } else {
        log.info("Work {} is done by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        persist(WorkCompleted(workId, result)) { event =>
          executionPlan = executionPlan.updated(event)
          mediator ! DistributedPubSubMediator.Publish(ResultsTopic, WorkResult(workId, result))
          sender ! WorkDoneAck(workId)
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

    case Heartbeat =>
      jobStore.pollOverdueJobs(clock, jobBatchSize) { jobDef =>
        val work = jobStore.createWork(jobDef)
        if (!executionPlan.isAccepted(work.id)) {
          log.info("Dispatching job to work queue. workId={}", work.id)
          persist(WorkAccepted(work)) { event =>
            executionPlan = executionPlan.updated(event)
            notifyWorkers()
          }
        }
      }

    case CleanupTick =>
      for ((workerId, workerState @ WorkerState(_, WorkerState.Busy(workId, timeout))) <- workers) {
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
