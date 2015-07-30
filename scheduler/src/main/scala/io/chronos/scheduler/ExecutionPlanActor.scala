package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}
import javax.cache.processor.{EntryProcessor, MutableEntry}

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.pattern._
import akka.util.Timeout
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import io.chronos.protocol.{SchedulerProtocol, _}
import io.chronos.{Execution, JobSchedule, JobSpec, topic}
import org.apache.ignite.Ignite

import scala.collection.JavaConversions._
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 26/07/15.
 */
object ExecutionPlanActor {

  private case object Heartbeat

}

class ExecutionPlanActor(ignite: Ignite, heartbeatInterval: FiniteDuration, sweepBatchLimit: Int)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import ExecutionPlanActor._
  import SchedulerProtocol._
  import context.dispatcher

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topic.Executions, self)

  private val beating = ignite.atomicReference("beating", false, true)

  private val scheduleCounter = ignite.atomicSequence("scheduleCounter", 0, true)
  private val scheduleMap = ignite.getOrCreateCache[ScheduleId, JobSchedule]("scheduleMap")

  private val executionCounter = ignite.atomicSequence("executionCounter", 0, true)
  private val executionMap = ignite.getOrCreateCache[ExecutionId, Execution]("executions")
  private val executionBySchedule = ignite.getOrCreateCache[ScheduleId, ExecutionId]("executionBySchedule")

  private val registryRef = context.system.actorSelection(context.system / "registry")
  private val queueRef = context.system.actorSelection(context.system / "execution" / "queue")

  def receive = {
    case ScheduleJob(schedule) =>
      implicit val timeout = Timeout(5 seconds)
      (registryRef ? GetJobSpec(schedule.jobId)).mapTo[Option[JobSpec]].map {
        case Some(jobSpec) =>
          val scheduleId = (schedule.jobId, scheduleCounter.incrementAndGet())
          scheduleMap.put(scheduleId, schedule)
          ScheduleJobAck(defineExecutionFor(scheduleId).executionId)

        case _ => ScheduleJobFailed(Left(JobNotRegistered(schedule.jobId)))
      } recover {
        case e: Throwable => ScheduleJobFailed(Right(e))
      } pipeTo sender()

    case GetSchedule(scheduleId) =>
      sender ! Option(scheduleMap.get(scheduleId))

    case ExecutionEvent(executionId, stage: Execution.Started) =>
      val execution = executionMap.invoke(executionId, new EntryProcessor[ExecutionId, Execution, Execution] {
        override def process(entry: MutableEntry[((JobId, Long), Long), Execution], arguments: AnyRef*): Execution = {
          val execution = entry.getValue << stage
          entry.setValue(execution)
          execution
        }
      })
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, stage)

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
        queueRef ! ExecutionQueueActor.Enqueue(updatedExec)

        itemCount += 1  // This awful statement is there to help the upper helper functions to build the batch
      }

      // Reset the atomic boolean flag to allow for more "beats"
      beating.set(false)
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

}
