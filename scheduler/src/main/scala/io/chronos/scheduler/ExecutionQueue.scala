package io.chronos.scheduler

import io.chronos.id._
import io.chronos.{JobSpec, Schedule}

/**
 * Created by domingueza on 04/08/15.
 */
trait ExecutionQueue {

  def hasPending: Boolean

  def dequeue(f: (ExecutionId, Schedule, JobSpec) => Unit): Unit
  
  def enqueue(executionId: ExecutionId): Unit
  
}