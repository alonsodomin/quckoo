package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.contrib.pattern._
import io.chronos.id._
import io.chronos.protocol.{SchedulerProtocol, WorkerProtocol}
import io.chronos.worker.WorkerState
import io.chronos.{Execution, Work, WorkResult, topic}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Scheduler {

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


class Scheduler(implicit clock: Clock,
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

    case GetExecutions(filter) =>
      sender() ! Executions(jobRegistry.executions(filter))

    case ScheduleJob(jobDef) =>
      log.info("Job scheduled. jobId={}", jobDef.jobId)
      val execution = jobRegistry.schedule(jobDef)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
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
        mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerRegistered(workerId))
      }

    case RequestWork(workerId) =>
      if (jobRegistry.hasPendingExecutions) {
        workers.get(workerId) match {
          case Some(workerState @ WorkerState(_, WorkerState.Idle)) =>
            val execution = jobRegistry.nextExecution

            val work = for (
              schedule <- jobRegistry.scheduleOf(execution.executionId);
              spec     <- jobRegistry.specOf(execution.executionId)
            ) yield Work(execution.executionId, schedule.params, spec.moduleId, spec.jobClass)

            log.info("Delivering execution to worker. executionId={}, workerId={}", execution.executionId, workerId)
            sender() ! work.get

            val executionStatus = Execution.Started(ZonedDateTime.now(clock), workerId)
            jobRegistry.updateExecution(execution.executionId, executionStatus) { exec =>
              val deadline = executionDeadline(exec)
              workers += (workerId -> workerState.copy(status = WorkerState.Busy(exec.executionId, deadline)))
              mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, executionStatus))
            }

          case _ =>
        }
      }

    case WorkDone(workerId, executionId, result) =>
      jobRegistry.executionById(executionId).map(_.stage) match {
        case Some(_: Execution.Finished) =>
          // previous Ack was lost, confirm again that this is done
          sender() ! WorkDoneAck(executionId)
        case Some(_: Execution.InProgress) =>
          log.info("Execution {} is done by worker {}", executionId, workerId)
          changeWorkerToIdle(workerId, executionId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Success(result))
          jobRegistry.updateExecution(executionId, executionStatus) { exec =>
            mediator ! DistributedPubSubMediator.Publish(topic.AllResults, WorkResult(exec.executionId, result))
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, executionStatus))
            sender() ! WorkDoneAck(exec.executionId)
          }
        case _ =>
          log.warning("Received a WorkDone notification for non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case WorkFailed(workerId, executionId, cause) =>
      jobRegistry.executionById(executionId).map(_.stage) match {
        case Some(_: Execution.InProgress) =>
          log.info("Execution {} failed by worker {}", executionId, workerId)
          changeWorkerToIdle(workerId, executionId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Failed(cause))
          jobRegistry.updateExecution(executionId, executionStatus) { exec =>
            // TODO implement a retry logic
            notifyWorkers()
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, executionStatus))
          }

        case _ =>
          log.info("Received a failed notification for a non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case Heartbeat =>
      jobRegistry.fetchOverdueExecutions(jobBatchSize) { execution =>
        log.info("Placing execution into work queue. executionId={}", execution.executionId)
        val executionStatus = Execution.Triggered(ZonedDateTime.now(clock))
        jobRegistry.updateExecution(execution.executionId, executionStatus) { exec =>
          notifyWorkers()
          mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, executionStatus))
        }
      }

    case CleanupTick =>
      for ((workerId, workerState @ WorkerState(_, WorkerState.Busy(executionId, timeout))) <- workers) {
        if (timeout.isOverdue()) {
          log.info("Execution {} at worker {} timed out!", executionId, workerId)
          workers -= workerId
          mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerUnregistered(workerId))
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.TimedOut)
          jobRegistry.updateExecution(executionId, executionStatus) { exec =>
            notifyWorkers()
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, executionStatus))
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
