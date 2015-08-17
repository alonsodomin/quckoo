package io.chronos

import java.util.UUID

import io.chronos.protocol.ResolutionFailed

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object cluster {

  type WorkerId = UUID
  type TaskFailureCause = Either[ResolutionFailed, Throwable]

  sealed trait WorkerEvent
  case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
  case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent

}
