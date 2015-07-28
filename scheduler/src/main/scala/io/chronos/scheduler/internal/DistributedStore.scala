package io.chronos.scheduler.internal

import java.time.{Clock, ZonedDateTime}

import io.chronos.id.ExecutionId
import io.chronos.{Execution, JobSchedule}
import org.apache.ignite.Ignite

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by aalonsodominguez on 27/07/15.
 */
class DistributedStore(val ignite: Ignite, val queueCapacity: Int) extends DistributedJobRegistry
                                                                      with DistributedExecutionCache
                                                                      with DistributedScheduleCache
                                                                      with DistributedExecutionQueue {

  def schedule(jobSchedule: JobSchedule)(implicit clock: Clock, ec: ExecutionContext): Future[Execution] =
    hasJob(jobSchedule.jobId).flatMap { isThere =>
      if (isThere) newSchedule(jobSchedule).flatMap(sid => newExecution(sid))
      else Future.failed[Execution](new IllegalArgumentException(s"The job specification ${jobSchedule.jobId} has not been registered yet."))
    }

  def sweepOverdue(f: Execution => Unit)(implicit clock: Clock, ec: ExecutionContext): Unit = {
    val now = ZonedDateTime.now(clock)
    scanSchedules((scheduleId, schedule) => {
      referenceTime(scheduleId) map {
        case Some(refTime) => schedule.trigger.nextExecutionTime(refTime)
        case _             => None
      } flatMap {
        case Some(nextTime) if nextTime.isBefore(now) || nextTime.isEqual(now) =>
          executionBySchedule(scheduleId).flatMap {
            case Some(execId) => executionById(execId)
            case _            => Future.successful(None)
          } map {
            case Some(x) => f(x)
            case _       => ()
          }

        case _ => Future.successful()
      }
    })
  }

  def executionUpdated(executionId: ExecutionId, newStage: Execution.Stage)(f: Execution => Unit)(implicit ec: ExecutionContext): Unit = {
    // Returns a future flagging whether we need to invoke the consumer function or not
    def updateCache(execution: Execution): Future[Boolean] = {
      newStage match {
        case _: Execution.Triggered =>
          enqueue(execution).map { _ => true }
        case _: Execution.Finished =>
          // TODO add the ability to re-schedule recurring jobs
          Future.successful(true)
        case _ => Future.successful(false)
      }
    }

    executionById(executionId) map {
      case Some(exec) => exec << newStage
    } map { exec =>
      updateCache(exec) map { invoke => if (invoke) f(exec) }
    }
  }

}
