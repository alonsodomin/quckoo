package io.chronos.scheduler.internal.cache

import java.time.{Clock, ZonedDateTime}

import com.hazelcast.core.HazelcastInstance
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 05/08/15.
 */
trait CacheAccessors extends CacheStructures {
  def grid: HazelcastInstance

  final def getSchedule(scheduleId: ScheduleId): Option[Schedule] =
    Option(scheduleMap.get(scheduleId))

  final def getExecution(executionId: ExecutionId): Option[Execution] =
    Option(executionMap.get(executionId))

  final def currentExecutionOf(scheduleId: ScheduleId): Option[ExecutionId] =
    Option(executionsBySchedule.get(scheduleId)) flatMap { _.headOption }

  def nextExecutionTime(scheduleId: ScheduleId)(implicit clock: Clock): Option[ZonedDateTime] =
    for {
      refTime <- referenceTime(scheduleId)
      schedule <- getSchedule(scheduleId)
      nextExec <- schedule.trigger.nextExecutionTime(refTime)
    } yield nextExec

  def referenceTime(scheduleId: ScheduleId): Option[ReferenceTime] = {
    import Execution._

    def executionAt[T <: Stage](scheduleId: ScheduleId)(implicit stage: StageLike[T]): Option[Execution] =
      Option(executionsBySchedule.get(scheduleId)) flatMap { execIds =>
        execIds.find { execId =>
          Option(executionMap.get(execId)).exists(stage <@ _)
        }
      } map executionMap.get

    executionAt[Finished](scheduleId).map { exec =>
      LastExecutionTime(exec.stage.when)
    } orElse executionAt[Scheduled](scheduleId).map { exec =>
      ScheduledTime(exec.stage.when)
    }
  }

}
