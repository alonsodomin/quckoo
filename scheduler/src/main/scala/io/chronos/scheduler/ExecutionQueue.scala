package io.chronos.scheduler

import io.chronos.id._

/**
 * Created by domingueza on 04/08/15.
 */
trait ExecutionQueue {

  def hasPending: Boolean

  def dequeue: ExecutionId
  
  def enqueue(executionId: ExecutionId): Unit
  
}
