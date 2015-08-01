package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}
import java.util.Map.Entry
import java.util.function.BiFunction

import akka.actor._
import akka.contrib.pattern.{ClusterReceptionistExtension, ClusterSingletonManager, DistributedPubSubExtension, DistributedPubSubMediator}
import akka.pattern._
import akka.util.Timeout
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.query.Predicate
import io.chronos.Execution._
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos._
import io.chronos.id._
import io.chronos.protocol.WorkerProtocol._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object SchedulerActor {

  val DefaultHearbeatInterval = 100 millis
  val DefaultWorkTimeout      = 5 minutes
  val DefaultSweepBatchLimit  = 50

  def props(hazelcastInstance: HazelcastInstance, registry: ActorRef,
            heartbeatInterval: FiniteDuration = DefaultHearbeatInterval,
            maxWorkTimeout: FiniteDuration = DefaultWorkTimeout,
            sweepBatchLimit: Int = DefaultSweepBatchLimit,
            role: Option[String] = None)(implicit clock: Clock): Props =
    ClusterSingletonManager.props(
      Props(classOf[SchedulerActor], hazelcastInstance, registry, heartbeatInterval, maxWorkTimeout, sweepBatchLimit, clock),
      "active", PoisonPill, role
    )

  private case object Heartbeat
  private case object CleanupBeat

}

class SchedulerActor(hazelcastInstance: HazelcastInstance, registry: ActorRef, heartbeatInterval: FiniteDuration,
                     maxWorkTimeout: FiniteDuration, sweepBatchLimit: Int)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import SchedulerActor._
  import context.dispatcher
  import io.chronos.protocol._

  ClusterReceptionistExtension(context.system).registerService(self)
  
  // References to other actors in the system
  private val mediator = DistributedPubSubExtension(context.system).mediator

  // Distributed data structures
  private val beating = hazelcastInstance.getAtomicReference[Boolean]("beating")
  beating.set(false)

  private val scheduleCounter = hazelcastInstance.getAtomicLong("scheduleCounter")
  private val scheduleMap = hazelcastInstance.getMap[ScheduleId, JobSchedule]("scheduleMap")

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val executionMap = hazelcastInstance.getMap[ExecutionId, Execution]("executions")
  private val executionsBySchedule = hazelcastInstance.getMap[ScheduleId, List[ExecutionId]]("executionsBySchedule")

  private val executionQueue = hazelcastInstance.getQueue[ExecutionId]("executionQueue")

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
      (registry ? GetJob(schedule.jobId)).mapTo[Option[JobSpec]].map {
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

    case RescheduleJob(scheduleId) =>
      val execution = defineExecutionFor(scheduleId)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(execution.executionId, execution.stage))
      sender ! ScheduleJobAck(execution.executionId)

    case GetSchedule(scheduleId) =>
      sender ! Option(scheduleMap.get(scheduleId))

    case GetScheduledJobs =>
      sender ! scheduleMap.entrySet().map(entry => (entry.getKey, entry.getValue)).toSeq

    case req: GetExecutions =>
      sender ! executionMap.values(new Predicate[ExecutionId, Execution] {
        override def apply(mapEntry: Entry[((JobId, Long), Long), Execution]): Boolean = req.filter(mapEntry.getValue)
      }).toSeq

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered: workerId={}", workerId)
        if (!executionQueue.isEmpty) {
          sender ! WorkReady
        }
        mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerRegistered(workerId))
      }

    case RequestWork(workerId) if !executionQueue.isEmpty =>
      workers.get(workerId) match {
        case Some(worker @ WorkerState(_, WorkerState.Idle)) =>
          val executionId = executionQueue.take()

          def futureJobSpec: Future[Option[JobSpec]] = {
            implicit val timeout = Timeout(5 seconds)
            (registry ? GetJob(executionId._1._1)).mapTo[Option[JobSpec]]
          }

          def futureSchedule: Future[Option[JobSchedule]] = Future { Option(scheduleMap.get(executionId._1)) }

          futureJobSpec.zip(futureSchedule).map(x => (x._1, x._2)).map {
            case (Some(jobSpec), Some(schedule)) =>
              def workTimeout: Deadline = Deadline.now + schedule.timeout.getOrElse(maxWorkTimeout)

              val executionStage = Execution.Started(ZonedDateTime.now(clock), workerId)
              updateExecutionAndApply(executionId, executionStage) { _ =>
                log.info("Delivering execution to worker. executionId={}, workerId={}", executionId, workerId)
                worker.ref ! Work(executionId, schedule.params, jobSpec.moduleId, jobSpec.jobClass)

                workers += (workerId -> worker.copy(status = WorkerState.Busy(executionId, workTimeout)))
                mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStage))
              }

            case _ =>
              log.error("Found a queued execution with no job or schedule association. executionId={}", executionId)

          } recover {
            case cause: Throwable =>
              log.error(cause, "Couldn't send execution to worker. Execution will be put back in the queue and retried later. executionId={}, workerId={}", executionId, workerId)
              executionQueue.offer(executionId)
              notifyWorkers()
          }

        case _ =>
          log.warning("Received a request of work from a worker that is not in idle state. workerId={}", workerId)
      }

    case WorkDone(workerId, executionId, result) =>
      Option(executionMap.get(executionId)).map(_.stage) match {
        case Some(_: Execution.Finished) =>
          // previous Ack was lost, confirm again that this is done
          workers(workerId).ref ! WorkDoneAck(executionId)
        case Some(_: Execution.Started) =>
          log.info("Execution {} is done by worker {}", executionId, workerId)
          val executionStatus = Execution.Finished(ZonedDateTime.now(clock), workerId, Execution.Success(result))
          updateExecutionAndApply(executionId, executionStatus) { _ =>
            workers(workerId).ref ! WorkDoneAck(executionId)
            changeWorkerToIdle(workerId, executionId)

            mediator ! DistributedPubSubMediator.Publish(topic.Results, WorkResult(executionId, result))
            mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(executionId, executionStatus))

            val (scheduleId, _) = executionId
            if (scheduleMap.get(scheduleId).isRecurring) {
              self ! RescheduleJob(scheduleId)
            }
          }

        case _ =>
          log.warning("Received a WorkDone notification for non in-progress execution. executionId={}, workerId={}", executionId, workerId)
      }

    case WorkFailed(workerId, executionId, cause) =>
      Option(executionMap.get(executionId)).map(_.stage) match {
        case Some(_: Execution.Started) =>
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

      def ready(scheduleId: ScheduleId): Boolean = (for {
        schedule <- Option(scheduleMap.get(scheduleId))
        execution <- currentExecutionOf(scheduleId)
      } yield execution is Execution.Ready).getOrElse(false)

      def schedules: Iterable[(ScheduleId, JobSchedule)] =
        scheduleMap.entrySet().view.
          filter(entry => ready(entry.getKey)).
          takeWhile(_ => underBatchLimit).
          map(entry => (entry.getKey, entry.getValue))

      def nextExecutionTime(scheduleId: ScheduleId, schedule: JobSchedule): Option[ZonedDateTime] =
        (for (time <- referenceTime(scheduleId)) yield schedule.trigger.nextExecutionTime(time)).flatten

      val now = ZonedDateTime.now(clock)
      for {
        (scheduleId, schedule) <- schedules
        nextTime <- nextExecutionTime(scheduleId, schedule) if nextTime.isBefore(now) || nextTime.isEqual(now)
        exec     <- currentExecutionOf(scheduleId)
      } updateExecutionAndApply(exec.executionId, Execution.Triggered(ZonedDateTime.now(clock))) { exec =>
        log.info("Placing execution into work queue. executionId={}", exec.executionId)
        executionQueue.offer(exec.executionId)
        mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, exec.stage))
        notifyWorkers()

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
          }
        }
      }
  }

  private def currentExecutionOf(scheduleId: ScheduleId): Option[Execution] =
    Option(executionsBySchedule.get(scheduleId)) flatMap { _.headOption } map executionMap.get

  private def referenceTime(scheduleId: ScheduleId): Option[ReferenceTime] = {
    def executionAt(scheduleId: ScheduleId, stage: StageLike[_]): Option[Execution] =
      Option(executionsBySchedule.get(scheduleId)) flatMap { execIds =>
        execIds.find { execId =>
          Option(executionMap.get(execId)).exists(stage.currentIn)
        }
      } map executionMap.get

    executionAt(scheduleId, Execution.Done).map { exec =>
      LastExecutionTime(exec.stage.when)
    } orElse executionAt(scheduleId, Execution.Ready).map { exec =>
      ScheduledTime(exec.stage.when)
    }
  }

  private def defineExecutionFor(scheduleId: ScheduleId): Execution = {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution(executionId)
    executionMap.put(executionId, execution)
    executionsBySchedule.merge(scheduleId, List(executionId), new BiFunction[List[ExecutionId], List[ExecutionId], List[ExecutionId]] {
      override def apply(oldValue: List[ExecutionId], newValue: List[ExecutionId]): List[ExecutionId] =
        if (oldValue == null || oldValue.isEmpty) newValue
        else newValue ::: oldValue
    })
    execution
  }

  private def notifyWorkers(): Unit = {
    if (!executionQueue.isEmpty) {
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

  private def updateExecutionAndApply[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): Future[T] = Future {
    executionMap.lock(executionId)
    try {
      val execution = executionMap.get(executionId) << stage
      executionMap.put(executionId, execution)
      f(execution)
    } finally {
      executionMap.unlock(executionId)
    }
  }

}
