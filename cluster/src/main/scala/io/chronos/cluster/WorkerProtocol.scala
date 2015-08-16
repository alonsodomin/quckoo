package io.chronos.cluster

import io.chronos.id.ExecutionId
import io.chronos.protocol._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  case class RegisterWorker(workerId: WorkerId)
  case class RequestWork(workerId: WorkerId)
  case class WorkDone(workerId: WorkerId, executionId: ExecutionId, result: Any)
  case class WorkFailed(workerId: WorkerId, executionId: ExecutionId, cause: ExecutionFailedCause)

  // Messages to workers
  case object WorkReady
  case class WorkDoneAck(executionId: ExecutionId)
}
