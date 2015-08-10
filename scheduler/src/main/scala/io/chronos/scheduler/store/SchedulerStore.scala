package io.chronos.scheduler.store

import io.chronos.Execution

import scala.collection.immutable.Queue

/**
 * Created by aalonsodominguez on 10/08/15.
 */
case class SchedulerStore private (private val pendingExecutions: Queue[Execution]) {

}
