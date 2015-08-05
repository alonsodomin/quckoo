package io.chronos.scheduler.internal.fun

import java.time.ZonedDateTime

import io.chronos.id.ScheduleId
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 05/08/15.
 */
class OverdueSchedulesFilter(clockDef: String) extends AliveSchedulesQuery(clockDef) {

  override def filterSchedule(scheduleId: ScheduleId, schedule: Schedule, execTime: ZonedDateTime): Boolean = {
    def ready(scheduleId: ScheduleId): Boolean = (for {
      schedule <- Option(scheduleMap.get(scheduleId))
      execution <- currentExecutionOf(scheduleId).flatMap(getExecution)
    } yield execution is Execution.Ready).getOrElse(false)

    val now = ZonedDateTime.now(clock)
    ready(scheduleId) && (execTime.isBefore(now) || execTime.isEqual(now))
  }

}
