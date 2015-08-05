package io.chronos.scheduler.internal.cache

import java.time.Clock
import java.util.{Comparator, Map => JMap}

import com.hazelcast.query.{PagingPredicate, Predicate}
import io.chronos.Execution
import io.chronos.id._
import io.chronos.scheduler.ExecutionCache
import io.chronos.scheduler.internal.fun.OverdueSchedulesFilter

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait HazelcastExecutionCache extends ExecutionCache with CacheStructures with CacheAccessors {
  import io.chronos.scheduler.internal.Implicits._

  def overdueExecutions(implicit clock: Clock): Traversable[ExecutionId] = {
    val predicate = new OverdueSchedulesFilter(clock.toString)

    val paging = new PagingPredicate(
      predicate.asInstanceOf[Predicate[_, _]],
      predicate.asInstanceOf[Comparator[JMap.Entry[_, _]]],
      50
    )

    DistributedTraversable(scheduleMap, paging).
      map(schedulePair => currentExecutionOf(schedulePair._1).get)
  }

  override def getExecutions(f: Execution => Boolean): Traversable[Execution] = {
    val ordering: Ordering[(ExecutionId, Execution)] = Ordering.by(_._2.lastStatusChange)
    def filter(executionId: ExecutionId, execution: Execution): Boolean = f(execution)
    DistributedTraversable(executionMap, ordering, 50, filter).map(_._2)
  }

  override def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): T = {
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
