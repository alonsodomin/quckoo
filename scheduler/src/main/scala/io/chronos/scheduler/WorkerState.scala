package io.chronos.scheduler

import akka.actor.ActorRef
import io.chronos.id.ExecutionId

import scala.concurrent.duration.Deadline

/**
 * Created by domingueza on 06/07/15.
 */

object WorkerState {

  sealed trait WorkerStatus

  case object Idle extends WorkerStatus
  case class Busy(executionId: ExecutionId, deadline: Deadline) extends WorkerStatus

}

case class WorkerState(ref: ActorRef, status: WorkerState.WorkerStatus)
