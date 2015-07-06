package io.chronos.scheduler.worker

import akka.actor.ActorRef

import scala.concurrent.duration.Deadline

/**
 * Created by domingueza on 06/07/15.
 */

object WorkerState {

  sealed trait WorkerStatus

  case object Idle extends WorkerStatus
  case class Busy(workId: String, deadline: Deadline) extends WorkerStatus

}

case class WorkerState(ref: ActorRef, status: WorkerState.WorkerStatus)
