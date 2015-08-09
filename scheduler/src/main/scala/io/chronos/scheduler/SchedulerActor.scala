package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import akka.actor._
import akka.contrib.pattern._
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos._
import io.chronos.id._
import io.chronos.protocol.WorkerProtocol._
import io.chronos.scheduler.cache._
import io.chronos.scheduler.concurrent.ClusterSync

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object SchedulerActor {

  val DefaultHeartbeatInterval = 1 seconds
  val DefaultWorkTimeout       = 5 minutes

  def props(executionPlan: ActorRef, jobCache: JobCache, scheduleCache: ScheduleCache,
            executionCache: ExecutionCache, executionQueue: ExecutionQueue,
            clusterSync: ClusterSync,
            heartbeatInterval: FiniteDuration = DefaultHeartbeatInterval,
            maxWorkTimeout: FiniteDuration = DefaultWorkTimeout,
            role: Option[String] = None)(implicit clock: Clock): Props =
    ClusterSingletonManager.props(
      Props(classOf[SchedulerActor], executionPlan, jobCache, scheduleCache, executionCache, executionQueue, clusterSync,
        heartbeatInterval, maxWorkTimeout, clock
      ), "active", PoisonPill, role
    )

  private case object Heartbeat
  private case object CleanupBeat

}

class SchedulerActor(executionPlan: ActorRef, jobCache: JobCache, scheduleCache: ScheduleCache,
                     executionCache: ExecutionCache, executionQueue: ExecutionQueue,
                     clusterSync: ClusterSync, heartbeatInterval: FiniteDuration,
                     maxWorkTimeout: FiniteDuration)(implicit clock: Clock)
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
    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered. workerId={}", workerId)
        if (executionQueue.hasPending) {
          sender ! WorkReady
        }
        mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerRegistered(workerId))
      }

    case RequestWork(workerId) if executionQueue.hasPending =>
      workers.get(workerId) match {
        case Some(worker @ WorkerState(_, WorkerState.Idle)) =>
          for {
            executionId <- Some(executionQueue.dequeue)
            schedule <- scheduleCache.get(executionId._1)
            jobSpec <- jobCache.get(executionId._1._1)
          } {
            val executionStage = Execution.Started(ZonedDateTime.now(clock), workerId)
            executionCache.update(executionId) { _ << executionStage } onSuccess { case Some(exec) =>
              def workTimeout: Deadline = Deadline.now + schedule.timeout.getOrElse(maxWorkTimeout)

              log.debug("Delivering execution to worker. executionId={}, workerId={}", executionId, workerId)
              worker.ref ! Work(executionId, schedule.params, jobSpec.moduleId, jobSpec.jobClass)

              workers += (workerId -> worker.copy(status = WorkerState.Busy(executionId, workTimeout)))
              mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStage))
            }
          }
          
        case _ =>
          log.warning("Received a request of work from a worker that is not in idle state. workerId={}", workerId)
      }

    case WorkDone(workerId, executionId, result) =>
      executionCache.get(executionId).map(_.stage) match {
        case Some(_: Execution.Finished) =>
          // previous Ack was lost, confirm again that this is done
          workers(workerId).ref ! WorkDoneAck(executionId)
        case Some(_: Execution.Started) =>
          log.debug("Worker has finished given execution. workerId={}, executionId={}", workerId, executionId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Success(result))
          executionCache.update(executionId) { _ << executionStatus } onSuccess { case Some(_) =>
            workers(workerId).ref ! WorkDoneAck(executionId)
            changeWorkerToIdle(workerId, executionId)

            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))

            val (scheduleId, _) = executionId
            if (scheduleCache(scheduleId).isRecurring) {
              executionPlan ! RescheduleJob(scheduleId)
            } else {
              scheduleCache.deactivate(scheduleId)
            }
          }

        case _ =>
          log.warning("Received a WorkDone notification for non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case WorkFailed(workerId, executionId, cause) =>
      executionCache.get(executionId).map(_.stage) match {
        case Some(_: Execution.Started) =>
          log.error("Worker has failed given execution. workerId={}, executionId={}", workerId, executionId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Failed(cause))
          executionCache.update(executionId) { _ << executionStatus } onSuccess { case Some(execution) =>
            // TODO implement a retry logic
            changeWorkerToIdle(workerId, executionId)
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
            notifyWorkers()
          }

        case _ =>
          log.warning("Received a WorkFailed notification for a non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case Heartbeat => clusterSync.synchronize {
      def ready(scheduleId: ScheduleId): Boolean = (for {
        schedule <- scheduleCache.get(scheduleId)
        execution <- executionCache.current(scheduleId).map(executionCache)
      } yield execution is Execution.Ready).getOrElse(false)

      val now = ZonedDateTime.now(clock)
      scheduleCache.active.filter { case (scheduleId, schedule) =>
          nextExecutionTime(scheduleId) match {
            case Some(time) if time.isBefore(now) || time.isEqual(now) => ready(scheduleId)
            case _ => false
          }
      } map { entry => executionCache.current(entry._1).get } foreach { executionId =>
        executionCache.update(executionId) { _ << Execution.Triggered(ZonedDateTime.now(clock)) } onSuccess { case Some(exec) =>
          log.debug("Placing execution into pending queue. executionId={}", executionId)
          executionQueue.enqueue(exec.executionId)
          mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, exec.stage))
          notifyWorkers()
        }
      }
    }

    case CleanupBeat =>
      for ((workerId, workerState @ WorkerState(_, WorkerState.Busy(executionId, timeout))) <- workers) {
        if (timeout.isOverdue()) {
          log.warning("Worker has timed out whilst running giving execution. workerId={}, executionId={}", workerId, executionId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.TimedOut)
          executionCache.update(executionId) { _ << executionStatus } onSuccess { case Some(_) =>
            workers -= workerId
            mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerUnregistered(workerId))
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
          }
        }
      }
  }

  private def notifyWorkers(): Unit = {
    if (executionQueue.hasPending) {
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

  private def nextExecutionTime(scheduleId: ScheduleId): Option[ZonedDateTime] =
    for {
      refTime <- referenceTime(scheduleId)
      schedule <- scheduleCache.get(scheduleId)
      nextExec <- schedule.trigger.nextExecutionTime(refTime)
    } yield nextExec

  private def referenceTime(scheduleId: ScheduleId): Option[ReferenceTime] = {
    import Execution._

    executionCache.which[Finished](scheduleId).map { exec =>
      LastExecutionTime(exec.stage.when)
    } orElse executionCache.which[Scheduled](scheduleId).map { exec =>
      ScheduledTime(exec.stage.when)
    }
  }

}
