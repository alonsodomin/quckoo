package io.chronos.scheduler

import io.chronos.Execution

/**
 * Created by aalonsodominguez on 12/07/15.
 */
trait ExecutionQueue {

  def hasPendingExecutions: Boolean

  def nextExecution: Execution

}
