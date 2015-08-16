package io.chronos

import java.util.UUID

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object cluster {

  type WorkerId = UUID

  sealed trait WorkerEvent
  case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
  case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent

}
