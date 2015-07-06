package io.chronos.scheduler.protocol

import io.chronos.scheduler.id.{WorkId, WorkerId}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  case class RegisterWorker(workerId: WorkerId)
  case class RequestWork(workerId: WorkerId)
  case class WorkDone(workerId: WorkerId, workId: WorkId, result: Any)
  case class WorkFailed(workerId: WorkerId, workId: WorkId)

  // Messages to workers
  case object WorkReady
  case class WorkDoneAck(workId: WorkId)
}
