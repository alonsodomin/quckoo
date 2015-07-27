package io.chronos.scheduler.internal

import io.chronos.Execution
import io.chronos.scheduler.ExecutionQueue
import org.apache.ignite.Ignite

/**
 * Created by aalonsodominguez on 27/07/15.
 */
trait DistributedExecutionQueue extends ExecutionQueue {
  protected implicit val ignite: Ignite
  protected implicit val queueCapacity: Int

  private val executionQueue = ignite.queue[Execution]("executionQueue", queueCapacity, null)

  override def hasPendingExecutions: Boolean = !executionQueue.isEmpty

  override def nextExecution: Execution = executionQueue.take()
  
}
