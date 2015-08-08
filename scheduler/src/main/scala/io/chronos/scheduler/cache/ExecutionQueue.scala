package io.chronos.scheduler.cache

import com.hazelcast.core.HazelcastInstance
import io.chronos.id._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
class ExecutionQueue(grid: HazelcastInstance) {

  private lazy val executionQueue = grid.getQueue[ExecutionId]("executionQueue")

  def hasPending: Boolean = !executionQueue.isEmpty

  def enqueue(executionId: ExecutionId): Unit = executionQueue.offer(executionId)

  def dequeue: ExecutionId = executionQueue.take()

}
