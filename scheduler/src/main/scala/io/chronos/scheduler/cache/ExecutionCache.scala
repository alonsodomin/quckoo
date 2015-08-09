package io.chronos.scheduler.cache

import java.time.Clock
import java.util
import java.util.function.BiFunction

import com.hazelcast.core.HazelcastInstance
import io.chronos.Execution
import io.chronos.Execution.{Stage, StageLike}
import io.chronos.id._

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 08/08/15.
 */
object ExecutionCache {

  type ExecutionEntry = (ExecutionId, Execution)

}

class ExecutionCache(grid: HazelcastInstance) extends DistributedCache[ExecutionId, Execution] {
  import ExecutionCache._

  private lazy val executionCounter = grid.getAtomicLong("executionCounter")
  private lazy val executionMap = grid.getMap[ExecutionId, Execution]("executions")
  private lazy val executionsBySchedule = grid.getMap[ScheduleId, List[ExecutionId]]("executionsBySchedule")

  def get(executionId: ExecutionId): Option[Execution] =
    Option(executionMap.get(executionId))

  def apply(executionId: ExecutionId): Execution =
    get(executionId).get

  def +=(scheduleId: ScheduleId)(implicit clock: Clock): Execution = {
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

  def current(scheduleId: ScheduleId): Option[ExecutionId] =
    Option(executionsBySchedule.get(scheduleId)) flatMap { _.headOption }

  def which[T <: Stage](scheduleId: ScheduleId)(implicit stage: StageLike[T]): Option[Execution] =
    Option(executionsBySchedule.get(scheduleId)) flatMap { execIds =>
      execIds.find { execId =>
        Option(executionMap.get(execId)).exists(_ @: stage)
      }
    } map executionMap.get

  def update(executionId: ExecutionId)(f: Execution => Execution): Option[Execution] = {
    executionMap.lock(executionId)
    try {
      get(executionId).map { exec =>
        val updated = f(exec)
        executionMap.put(executionId, updated)
        updated
      }
    } finally {
      executionMap.unlock(executionId)
    }
  }

  def toTraversable: Traversable[ExecutionEntry] = {
    val traversable = new Traversable[ExecutionEntry] {
      val values: util.Collection[util.Map.Entry[ExecutionId, Execution]] = executionMap.entrySet()

      override def foreach[U](f: ExecutionEntry => U) =
        for (entry <- values) f(entry.getKey, entry.getValue)
    }
    traversable.view
  }

}
