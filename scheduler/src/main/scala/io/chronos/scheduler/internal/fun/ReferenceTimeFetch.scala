package io.chronos.scheduler.internal.fun

import com.hazelcast.core.HazelcastInstance
import io.chronos.Execution
import io.chronos.Trigger.{LastExecutionTime, ReferenceTime, ScheduledTime}
import io.chronos.id._
import io.chronos.scheduler.internal.cache.CacheStructures

/**
 * Created by aalonsodominguez on 04/08/15.
 */
class ReferenceTimeFetch(val hazelcastInstance: HazelcastInstance)
  extends ((ScheduleId) => Option[ReferenceTime]) with CacheStructures {

  override def apply(scheduleId: ScheduleId): Option[ReferenceTime] = {
    import Execution._

    def executionAt[T <: Stage](scheduleId: ScheduleId)(implicit stage: StageLike[T]): Option[Execution] =
      Option(executionsBySchedule.get(scheduleId)) flatMap { execIds =>
        execIds.find { execId =>
          Option(executionMap.get(execId)).exists(stage.currentIn)
        }
      } map executionMap.get

    executionAt[Finished](scheduleId).map { exec =>
      LastExecutionTime(exec.stage.when)
    } orElse executionAt[Scheduled](scheduleId).map { exec =>
      ScheduledTime(exec.stage.when)
    }
  }
}
