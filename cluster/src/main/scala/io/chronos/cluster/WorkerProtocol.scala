package io.chronos.cluster

import java.util.UUID

import io.chronos.protocol._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  case class RegisterWorker(workerId: WorkerId)
  case class RequestWork(workerId: WorkerId)
  case class WorkDone(workerId: WorkerId, executionId: UUID, result: Any)
  case class WorkFailed(workerId: WorkerId, executionId: UUID, cause: ExecutionFailedCause)

  // Messages to workers
  case object WorkReady
  case class WorkDoneAck(executionId: UUID)
}
