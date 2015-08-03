package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import akka.actor._
import akka.contrib.pattern._
import io.chronos._
import io.chronos.id._
import io.chronos.protocol.WorkerProtocol._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object SchedulerActor {

  val DefaultHearbeatInterval = 100 millis
  val DefaultWorkTimeout      = 5 minutes
  val DefaultSweepBatchLimit  = 50

  def props(executionPlan: ExecutionPlan, registry: ActorRef,
            heartbeatInterval: FiniteDuration = DefaultHearbeatInterval,
            maxWorkTimeout: FiniteDuration = DefaultWorkTimeout,
            sweepBatchLimit: Int = DefaultSweepBatchLimit,
            role: Option[String] = None)(implicit clock: Clock): Props =
    ClusterSingletonManager.props(
      Props(classOf[SchedulerActor], executionPlan, registry, heartbeatInterval, maxWorkTimeout, sweepBatchLimit, clock),
      "active", PoisonPill, role
    )

  private case object Heartbeat
  private case object CleanupBeat

}

class SchedulerActor(executionPlan: ExecutionPlan, registry: ActorRef, heartbeatInterval: FiniteDuration,
                     maxWorkTimeout: FiniteDuration, sweepBatchLimit: Int)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import SchedulerActor._
  import context.dispatcher
  import io.chronos.protocol._

  ClusterReceptionistExtension(context.system).registerService(self)
  private val mediator = DistributedPubSubExtension(context.system).mediator

  // Tasks
  private val cleanupTask = context.system.scheduler.schedule(maxWorkTimeout / 2, maxWorkTimeout / 2, self, CleanupBeat)
  private val heartbeatTask = context.system.scheduler.schedule(0 seconds, heartbeatInterval, self, Heartbeat)

  // Non-persistent state
  private var workers = Map.empty[WorkerId, WorkerState]

  override def postStop(): Unit = {
    cleanupTask.cancel()
    heartbeatTask.cancel()
  }

  def receive = {
    case ScheduleJob(schedule) =>
      val execution = executionPlan.schedule(schedule)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)

    case RescheduleJob(scheduleId) =>
      val execution = executionPlan.reschedule(scheduleId)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)

    case GetSchedule(scheduleId) =>
      sender ! executionPlan.getSchedule(scheduleId)

    case GetScheduledJobs =>
      sender ! executionPlan.getScheduledJobs

    case req: GetExecutions =>
      sender ! executionPlan.getExecutions(req.filter)

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered: workerId={}", workerId)
        if (executionPlan.hasPendingExecutions) {
          sender ! WorkReady
        }
        mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerRegistered(workerId))
      }

    case RequestWork(workerId) if executionPlan.hasPendingExecutions =>
      workers.get(workerId) match {
        case Some(worker @ WorkerState(_, WorkerState.Idle)) =>
          executionPlan.takePending { (executionId, jobSchedule, jobSpec) =>
            def workTimeout: Deadline = Deadline.now + jobSchedule.timeout.getOrElse(maxWorkTimeout)

            val executionStage = Execution.Started(ZonedDateTime.now(clock), workerId)
            executionPlan.updateExecution(executionId, executionStage) { exec =>
              log.info("Delivering execution to worker. executionId={}, workerId={}", executionId, workerId)
              worker.ref ! Work(executionId, jobSchedule.params, jobSpec.moduleId, jobSpec.jobClass)

              workers += (workerId -> worker.copy(status = WorkerState.Busy(executionId, workTimeout)))
              mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStage))
            }
          }

        case _ =>
          log.warning("Received a request of work from a worker that is not in idle state. workerId={}", workerId)
      }

    case WorkDone(workerId, executionId, result) =>
      executionPlan.getExecution(executionId).map(_.stage) match {
        case Some(_: Execution.Finished) =>
          // previous Ack was lost, confirm again that this is done
          workers(workerId).ref ! WorkDoneAck(executionId)
        case Some(_: Execution.Started) =>
          log.info("Execution {} is done by worker {}", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Success(result))
          executionPlan.updateExecution(executionId, executionStatus) { _ =>
            workers(workerId).ref ! WorkDoneAck(executionId)
            changeWorkerToIdle(workerId, executionId)

            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))

            val (scheduleId, _) = executionId
            executionPlan.getSchedule(scheduleId).filter(_.isRecurring).foreach { _ =>
              self ! RescheduleJob(scheduleId)
            }
          }

        case _ =>
          log.warning("Received a WorkDone notification for non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case WorkFailed(workerId, executionId, cause) =>
      executionPlan.getExecution(executionId).map(_.stage) match {
        case Some(_: Execution.Started) =>
          log.error("Execution {} failed by worker {}", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Failed(cause))
          executionPlan.updateExecution(executionId, executionStatus) { execution =>
            // TODO implement a retry logic
            changeWorkerToIdle(workerId, executionId)
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
            notifyWorkers()
          }

        case _ =>
          log.warning("Received a WorkFailed notification for a non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case Heartbeat =>
      executionPlan.sweepOverdueExecutions(sweepBatchLimit) { executionId =>
        log.info("Placing execution into work queue. executionId={}", executionId)
        executionPlan.updateExecution(executionId, Execution.Triggered(ZonedDateTime.now(clock))) { exec =>
          mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, exec.stage))
          notifyWorkers()
        }
      }

    case CleanupBeat =>
      for ((workerId, workerState @ WorkerState(_, WorkerState.Busy(executionId, timeout))) <- workers) {
        if (timeout.isOverdue()) {
          log.info("Execution {} at worker {} timed out!", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.TimedOut)
          executionPlan.updateExecution(executionId, executionStatus) { _ =>
            workers -= workerId
            mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerUnregistered(workerId))
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
          }
        }
      }
  }

  private def notifyWorkers(): Unit = {
    if (executionPlan.hasPendingExecutions) {
      def randomWorkers: Seq[(WorkerId, WorkerState)] = workers.toSeq

      randomWorkers.foreach {
        case (_, WorkerState(ref, WorkerState.Idle)) => ref ! WorkReady
        case _ => // busy
      }
    }
  }

  private def changeWorkerToIdle(workerId: WorkerId, executionId: ExecutionId): Unit = workers.get(workerId) match {
    case Some(workerState @ WorkerState(_, WorkerState.Busy(`executionId`, _))) =>
      workers += (workerId -> workerState.copy(status = WorkerState.Idle))
    case _ => // might happen after standby recovery, worker state is not persisted
  }

}
