package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}
import javax.cache.processor.{EntryProcessor, MutableEntry}

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.contrib.pattern.{ClusterReceptionistExtension, ClusterSingletonManager, DistributedPubSubExtension, DistributedPubSubMediator}
import akka.pattern._
import akka.util.Timeout
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos._
import io.chronos.id._
import io.chronos.protocol.WorkerProtocol._
import org.apache.ignite.Ignite
import org.apache.ignite.configuration.CollectionConfiguration

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object SchedulerActor {

  val DefaultHearbeatInterval = 100 millis
  val DefaultWorkTimeout      = 5 minutes
  val DefaultSweepBatchLimit  = 100
  val DefaultQueueCapacity    = 50

  def props(ignite: Ignite,
            heartbeatInterval: FiniteDuration = DefaultHearbeatInterval,
            maxWorkTimeout: FiniteDuration = DefaultWorkTimeout,
            sweepBatchLimit: Int = DefaultSweepBatchLimit,
            queueCapacity: Int = DefaultQueueCapacity,
            role: Option[String] = None)(implicit clock: Clock): Props =
    ClusterSingletonManager.props(
      Props(classOf[SchedulerActor], ignite, heartbeatInterval, maxWorkTimeout, sweepBatchLimit, queueCapacity, clock),
      "active", PoisonPill, role
    )

  private case object Heartbeat
  private case object CleanupBeat

}

class SchedulerActor(ignite: Ignite, heartbeatInterval: FiniteDuration, maxWorkTimeout: FiniteDuration,
                     sweepBatchLimit: Int, queueCapacity: Int)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import SchedulerActor._
  import context.dispatcher
  import io.chronos.protocol._

  ClusterReceptionistExtension(context.system).registerService(self)
  
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

  private val executionQueue = ignite.queue[ExecutionId]("executionQueue", queueCapacity, new CollectionConfiguration)

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
              updateExecutionAndApply(executionId, executionStage) { _ =>
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

        case _ =>
          log.warning("Received a request of work from a worker that is not in idle state. workerId={}", workerId)
      }

    case WorkDone(workerId, executionId, result) =>
      Option(executionMap.get(executionId)).map(_.stage) match {
        case Some(_: Execution.Finished) =>
          // previous Ack was lost, confirm again that this is done
          sender ! WorkDoneAck(executionId)
        case Some(_: Execution.InProgress) =>
          log.info("Execution {} is done by worker {}", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Success(result))
          updateExecutionAndApply(executionId, executionStatus) { _ =>
            sender ! WorkDoneAck(executionId)
            changeWorkerToIdle(workerId, executionId)
            mediator ! DistributedPubSubMediator.Publish(topic.AllResults, WorkResult(executionId, result))
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
          }

        case _ =>
          log.warning("Received a WorkDone notification for non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case WorkFailed(workerId, executionId, cause) =>
      Option(executionMap.get(executionId)).map(_.stage) match {
        case Some(_: Execution.InProgress) =>
          log.error("Execution {} failed by worker {}", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Failed(cause))
          updateExecutionAndApply(executionId, executionStatus) { execution =>
            // TODO implement a retry logic
            changeWorkerToIdle(workerId, executionId)
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))
            notifyWorkers()
          }

        case _ =>
          log.warning("Received a WorkFailed notification for a non in-progress execution. executionId={}, workerId={}", executionId, workerId)
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
      } {
        log.info("Placing execution into work queue. executionId={}", execId)
        updateExecutionAndApply(execId, Execution.Triggered(ZonedDateTime.now(clock))) { exec =>
          executionMap.put(execId, exec)
          executionQueue.put(execId)
          mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execId, exec.stage))

          itemCount += 1  // This awful statement is there to help the upper helper functions to build the batch
        }
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
