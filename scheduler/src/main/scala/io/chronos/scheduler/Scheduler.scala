package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.contrib.pattern.{ClusterReceptionistExtension, ClusterSingletonManager, DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.id._
import io.chronos.protocol.{SchedulerProtocol, WorkerProtocol}
import io.chronos.worker.WorkerState
import io.chronos.{Execution, Work, WorkResult}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Scheduler {

  val ResultsTopic = "results"

  val defaultHeartbeatInterval = 1000.millis
  val defaultBatchSize = 10
  val defaultMaxWorkTimeout = 1.minute

  def props(clock: Clock,
            jobRegistry: HazelcastJobRegistry,
            maxWorkTimeout: FiniteDuration = defaultMaxWorkTimeout,
            heartbeatInterval: FiniteDuration = defaultHeartbeatInterval,
            jobBatchSize: Int = defaultBatchSize,
            role: Option[String] = None): Props =
    ClusterSingletonManager.props(
      Props(classOf[Scheduler], clock, jobRegistry, maxWorkTimeout, heartbeatInterval, jobBatchSize),
      "active", PoisonPill, role
    )

  private case object Heartbeat
  private case object CleanupTick
}


class Scheduler(clock: Clock,
                jobRegistry: HazelcastJobRegistry,
                maxWorkTimeout: FiniteDuration,
                heartbeatInterval: FiniteDuration,
                jobBatchSize: Int)
  extends Actor with ActorLogging {

  import Scheduler._
  import SchedulerProtocol._
  import WorkerProtocol._
  import context.dispatcher

  val mediator = DistributedPubSubExtension(context.system).mediator
  ClusterReceptionistExtension(context.system).registerService(self)

  /* persistenceId must include cluster role to support multiple masters
  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("scheduler-")) match {
    case Some(role) => role + "-master"
    case _ => "master"
  }*/

  // worker state is not event sourced
  private var workers = Map[WorkerId, WorkerState]()

  // execution plan is event sourced
  //private var executionPlan = WorkQueue.empty

  private val cleanupTask = context.system.scheduler.schedule(maxWorkTimeout / 2, maxWorkTimeout / 2, self, CleanupTick)
  private val heartbeatTask = context.system.scheduler.schedule(0.seconds, heartbeatInterval, self, Heartbeat)

  override def postStop(): Unit = {
    cleanupTask.cancel()
    heartbeatTask.cancel()
  }

  def notifyWorkers(): Unit =
    if (jobRegistry.hasPendingExecutions) {
      workers.foreach {
        case (_, WorkerState(ref, WorkerState.Idle)) => ref ! WorkReady
        case _ => // busy
      }
    }

  def changeWorkerToIdle(workerId: WorkerId, executionId: ExecutionId): Unit =
    workers.get(workerId) match {
      case Some(workerState @ WorkerState(_, WorkerState.Busy(`executionId`, _))) =>
        workers += (workerId -> workerState.copy(status = WorkerState.Idle))
      case _ => // might happen after standby recovery, worker state is not persisted
    }

  def receive = {
    case GetScheduledJobs =>
      sender() ! ScheduledJobs(jobRegistry.scheduledJobs)

    case GetExecutions =>
      sender() ! Executions(jobRegistry.executions)

    case ScheduleJob(jobDef) =>
      log.info("Job scheduled. jobId={}", jobDef.jobId)
      jobRegistry.schedule(clock, jobDef)
      sender() ! ScheduleAck(jobDef.jobId)

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered: {}", workerId)
        if (jobRegistry.hasPendingExecutions) {
          sender() ! WorkReady
        }
      }

    case RequestWork(workerId) =>
      if (jobRegistry.hasPendingExecutions) {
        workers.get(workerId) match {
          case Some(workerState @ WorkerState(_, WorkerState.Idle)) =>
            val execution = jobRegistry.nextExecution
            val work = Work(executionId = execution.executionId,
              params = jobRegistry.scheduleOf(execution.executionId).params,
              jobClass = jobRegistry.specOf(execution.executionId).jobClass
            )

            log.info("Delivering execution to worker. executionId={}, workerId={}", execution.executionId, workerId)
            sender() ! work

            val executionEvent = Execution.Started(ZonedDateTime.now(clock), workerId)
            jobRegistry.updateExecution(execution.executionId, executionEvent) { exec =>
              val deadline = executionDeadline(exec)
              workers += (workerId -> workerState.copy(status = WorkerState.Busy(exec.executionId, deadline)))
            }

          case _ =>
        }
      }

    case WorkDone(workerId, executionId, result) =>
      jobRegistry.getExecution(executionId).map(e => e.status) match {
        case Some(_: Execution.Finished) =>
          // previous Ack was lost, confirm again that this is done
          sender() ! WorkDoneAck(executionId)
        case Some(_: Execution.InProgress) =>
          log.info("Execution {} is done by worker {}", executionId, workerId)
          changeWorkerToIdle(workerId, executionId)
          val executionEvent = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Success(result))
          jobRegistry.updateExecution(executionId, executionEvent) { exec =>
            mediator ! DistributedPubSubMediator.Publish(ResultsTopic, WorkResult(exec.executionId, result))
            sender() ! WorkDoneAck(exec.executionId)
          }
        case _ =>
          log.warning("Received a WorkDone notification for non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case WorkFailed(workerId, executionId, cause) =>
      jobRegistry.getExecution(executionId).map(e => e.status) match {
        case Some(_: Execution.InProgress) =>
          log.info("Execution {} failed by worker {}", executionId, workerId)
          changeWorkerToIdle(workerId, executionId)
          val executionEvent = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Failed(cause))
          jobRegistry.updateExecution(executionId, executionEvent) { exec =>
            // TODO implement a retry logic
            notifyWorkers()
          }

        case _ =>
          log.info("Received a failed notification for a non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case Heartbeat =>
      jobRegistry.fetchOverdueExecutions(clock, jobBatchSize) { execution =>
        val executionEvent = Execution.Triggered(ZonedDateTime.now(clock))
        log.info("Dispatching job execution queue. executionId={}", execution.executionId)
        jobRegistry.updateExecution(execution.executionId, executionEvent) { exec =>
          notifyWorkers()
        }
      }

    case CleanupTick =>
      for ((workerId, workerState @ WorkerState(_, WorkerState.Busy(executionId, timeout))) <- workers) {
        if (timeout.isOverdue()) {
          log.info("Execution {} at worker {} timed out!", executionId, workerId)
          workers -= workerId
          val executionEvent = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.TimedOut)
          jobRegistry.updateExecution(executionId, executionEvent) { exec =>
            notifyWorkers()
          }
        }
      }
  }

  private def executionDeadline(execution: Execution): Deadline = {
    val now = Deadline.now
    val timeout = jobRegistry.executionTimeout(execution.executionId) match {
      case Some(t) => t
      case None => maxWorkTimeout
    }
    now + timeout
  }

}
