package io.chronos.scheduler.internal

import io.chronos.Execution
import org.apache.ignite.Ignite

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by aalonsodominguez on 27/07/15.
 */
trait DistributedExecutionQueue {
  protected implicit val ignite: Ignite
  protected implicit val queueCapacity: Int

  private val executionQueue = ignite.queue[Execution]("executionQueue", queueCapacity, null)

  def hasPendingExecutions(implicit ec: ExecutionContext): Future[Boolean] = Future { !executionQueue.isEmpty }

  def nextExecution(implicit ec: ExecutionContext): Future[Execution] = Future { executionQueue.take() }

  protected def enqueue(execution: Execution)(implicit ec: ExecutionContext): Future[Unit] = Future { executionQueue.put(execution) }

}
