package io.chronos.protocol

import io.chronos.id.{ExecutionId, WorkerId}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  case class RegisterWorker(workerId: WorkerId)
  case class RequestWork(workerId: WorkerId)
  case class WorkDone(workerId: WorkerId, executionId: ExecutionId, result: Any)
  case class WorkFailed(workerId: WorkerId, executionId: ExecutionId, cause: Throwable)

  // Messages to workers
  case object WorkReady
  case class WorkDoneAck(executionId: ExecutionId)
}
