package io.chronos.scheduler.internal

import java.time.Clock

import io.chronos.Execution
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import org.apache.ignite.Ignite

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by aalonsodominguez on 27/07/15.
 */
trait DistributedExecutionCache {
  protected implicit val ignite: Ignite

  private val executionCounter = ignite.atomicSequence("executionCounter", 0, true)
  protected val executionMap = ignite.getOrCreateCache[ExecutionId, Execution]("executions")
  protected val executionBySchedule = ignite.getOrCreateCache[ScheduleId, ExecutionId]("executionBySchedule")

  def executionById(executionId: ExecutionId)(implicit ec: ExecutionContext): Future[Option[Execution]] =
    Future { Option(executionMap.get(executionId)) }

  def executionBySchedule(scheduleId: ScheduleId)(implicit ec: ExecutionContext): Future[Option[ExecutionId]] =
    Future { Option(executionBySchedule.get(scheduleId)) }

  def currentStageOf(executionId: ExecutionId)(implicit ec: ExecutionContext): Future[Option[Execution.Stage]] =
    Future { Option(executionMap.get(executionId)).map(_.stage) }

  protected def referenceTime(scheduleId: ScheduleId)(implicit ec: ExecutionContext): Future[Option[ReferenceTime]] =
    Future {
      Option(executionBySchedule.get(scheduleId)).flatMap(execId => Option(executionMap.get(execId))).map(_.stage).flatMap {
        case Execution.Scheduled(when)      => Some(ScheduledTime(when))
        case Execution.Finished(when, _, _) => Some(LastExecutionTime(when))
        case _                              => None
      }
    }

  protected def newExecution(scheduleId: ScheduleId)(implicit clock: Clock, ec: ExecutionContext): Future[Execution] = Future {
    val executionId = (scheduleId, executionCounter.incrementAndGet())
    val execution = Execution(executionId)
    executionMap.put(executionId, execution)
    executionBySchedule.put(scheduleId, executionId)
    execution
  }

}
