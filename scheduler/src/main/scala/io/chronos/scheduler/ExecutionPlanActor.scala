package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}
import javax.cache.processor.{EntryProcessor, MutableEntry}

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.pattern._
import akka.util.Timeout
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos._
import io.chronos.id._
import io.chronos.protocol.WorkerProtocol.{RegisterWorker, RequestWork, WorkReady}
import io.chronos.protocol.{SchedulerProtocol, _}
import org.apache.ignite.Ignite

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExecutionPlanActor {

  private case object Heartbeat
  private case object CleanupBeat

}

class ExecutionPlanActor(ignite: Ignite, heartbeatInterval: FiniteDuration, sweepBatchLimit: Int, queueCapacity: Int, maxWorkTimeout: FiniteDuration)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import ExecutionPlanActor._
  import SchedulerProtocol._
  import context.dispatcher

  // References to other actors in the system
  private val mediator = DistributedPubSubExtension(context.system).mediator
  private val registry = context.system.actorSelection(context.system / "registry")

  // Distributed data structures
  private val beating = ignite.atomicReference("beating", false, true)

  private val scheduleCounter = ignite.atomicSequence("scheduleCounter", 0, true)
  private val scheduleMap = ignite.getOrCreateCache[ScheduleId, JobSchedule]("scheduleMap")

  private val executionCounter = ignite.atomicSequence("executionCounter", 0, true)
  private val executionMap = ignite.getOrCreateCache[ExecutionId, Execution]("executions")
  private val executionBySchedule = ignite.getOrCreateCache[ScheduleId, ExecutionId]("executionBySchedule")

  private val executionQueue = ignite.queue[ExecutionId]("executionQueue", queueCapacity, null)

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
      implicit val timeout = Timeout(5 seconds)
      (registry ? GetJobSpec(schedule.jobId)).mapTo[Option[JobSpec]].map {
        case Some(jobSpec) =>
          val scheduleId = (schedule.jobId, scheduleCounter.incrementAndGet())
          scheduleMap.put(scheduleId, schedule)
          val execution = defineExecutionFor(scheduleId)
          mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
          ScheduleJobAck(execution.executionId)

        case _ => ScheduleJobFailed(Left(JobNotRegistered(schedule.jobId)))
      } recover {
        case e: Throwable => ScheduleJobFailed(Right(e))
      } pipeTo sender

    case GetSchedule(scheduleId) =>
      sender ! Option(scheduleMap.get(scheduleId))

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered: {}", workerId)
        if (!executionQueue.isEmpty) {
          sender ! WorkReady
        }
        mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerRegistered(workerId))
      }

    case RequestWork(workerId) if !executionQueue.isEmpty =>
      workers.get(workerId) match {
        case Some(workerState@WorkerState(_, WorkerState.Idle)) =>
          val executionId = executionQueue.take()

          def futureJobSpec: Future[Option[JobSpec]] = {
            implicit val timeout = Timeout(5 seconds)
            (registry ? GetJobSpec(executionId._1._1)).mapTo[Option[JobSpec]]
          }

          def futureSchedule: Future[Option[JobSchedule]] = Future { Option(scheduleMap.get(executionId._1)) }

          futureJobSpec.zip(futureSchedule).map(x => (x._1, x._2)).map {
            case (Some(jobSpec), Some(schedule)) =>
              def workTimeout: Deadline = Deadline.now + schedule.timeout.getOrElse(maxWorkTimeout)

              val executionStage = Execution.Started(ZonedDateTime.now(clock), workerId)
              updateExecutionAndApply(executionId, executionStage) { execution =>
                log.info("Delivering execution to worker. executionId={}, workerId={}", executionId, workerId)
                sender ! Work(executionId, schedule.params, jobSpec.moduleId, jobSpec.jobClass)

                workers += (workerId -> workerState.copy(status = WorkerState.Busy(executionId, workTimeout)))
                mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStage))
              }

            case _ =>
              log.error("Found a queued execution with no job or schedule association. executionId={}", executionId)

          } recover {
            case cause: Throwable =>
              log.error(cause, "Couldn't send execution to worker. Execution will be put back in the queue and retried later. executionId={}, workerId={}", executionId, workerId)
              executionQueue.put(executionId)
              notifyWorkers()
          }
      }

    case Heartbeat if beating.compareAndSet(false, true) =>
      var itemCount = 0
      def underBatchLimit: Boolean = itemCount < sweepBatchLimit

      def notInProgress(scheduleId: ScheduleId): Boolean =
        Option(executionBySchedule.get(scheduleId)).map(executionMap.get).map(_.stage) match {
        case Some(_: Execution.InProgress) => false
        case _                             => true
      }

      def localSchedules: Iterable[(ScheduleId, JobSchedule)] =
        scheduleMap.localEntries().view.
          filter(entry => notInProgress(entry.getKey)).
          takeWhile(_ => underBatchLimit).
          map(entry => (entry.getKey, entry.getValue))

      val now = ZonedDateTime.now(clock)
      for {
        (scheduleId, schedule) <- localSchedules
        nextTime <- nextExecutionTime(scheduleId, schedule) if nextTime.isBefore(now) || nextTime.isEqual(now)
        execId   <- Option(executionBySchedule.get(scheduleId))
        exec     <- Option(executionMap.get(execId))
      } {
        log.info("Placing execution into work queue. executionId={}", exec.executionId)
        val updatedExec = exec << Execution.Triggered(ZonedDateTime.now(clock))
        executionMap.put(execId, updatedExec)
        executionQueue.put(execId)
        mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execId, updatedExec.stage))

        itemCount += 1  // This awful statement is there to help the upper helper functions to build the batch
      }

      // Reset the atomic boolean flag to allow for more "beats"
      beating.set(false)

    case CleanupBeat =>
      for ((workerId, workerState @ WorkerState(_, WorkerState.Busy(executionId, timeout))) <- workers) {
        if (timeout.isOverdue()) {
          log.info("Execution {} at worker {} timed out!", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.TimedOut)
          updateExecutionAndApply(executionId, executionStatus) { _ =>
            workers -= workerId
            mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerUnregistered(workerId))
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
            notifyWorkers()
          }
        }
      }
  }

  private def nextExecutionTime(scheduleId: ScheduleId, schedule: JobSchedule): Option[ZonedDateTime] =
    (for (time <- referenceTime(scheduleId)) yield schedule.trigger.nextExecutionTime(time)).flatten

  private def referenceTime(scheduleId: ScheduleId): Option[ReferenceTime] =
    Option(executionBySchedule.get(scheduleId)).
      flatMap(execId => Option(executionMap.get(execId))).
      map(_.stage).flatMap {
      case Execution.Scheduled(when)      => Some(ScheduledTime(when))
      case Execution.Finished(when, _, _) => Some(LastExecutionTime(when))
      case _                              => None
    }

  private def defineExecutionFor(scheduleId: ScheduleId): Execution = {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution(executionId)
    executionMap.put(executionId, execution)
    executionBySchedule.put(scheduleId, executionId)
    execution
  }

  private def notifyWorkers(): Unit = {
    if (!executionQueue.isEmpty) {
      workers.foreach {
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

  private def updateExecutionAndApply[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): Future[T] = Future {
    executionMap.invoke(executionId, new EntryProcessor[ExecutionId, Execution, T] {
      override def process(entry: MutableEntry[((JobId, Long), Long), Execution], arguments: AnyRef*): T = {
        val execution = entry.getValue << stage
        entry.setValue(execution)
        f(execution)
      }
    })
  }

}
