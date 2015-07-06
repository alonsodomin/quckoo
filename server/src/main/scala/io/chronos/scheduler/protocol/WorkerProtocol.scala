package io.chronos.scheduler.protocol

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  case class RegisterWorker(workerId: String)
  case class RequestWork(workerId: String)
  case class WorkDone(workerId: String, workId: String, result: Any)
  case class WorkFailed(workerId: String, workId: String)

  // Messages to workers
  case object WorkReady
  case class WorkAck(id: String)
}
