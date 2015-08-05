package io.chronos.scheduler.internal.cache

import com.hazelcast.core.HazelcastInstance
import io.chronos.id._
import io.chronos.scheduler.ExecutionQueue

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait HazelcastExecutionQueue extends ExecutionQueue {

  val grid: HazelcastInstance

  private lazy val executionQueue = grid.getQueue[ExecutionId]("executionQueue")

  override final def hasPending: Boolean = !executionQueue.isEmpty

  override final def enqueue(executionId: ExecutionId): Unit = executionQueue.offer(executionId)

  override final def dequeue: ExecutionId = executionQueue.take()

}
