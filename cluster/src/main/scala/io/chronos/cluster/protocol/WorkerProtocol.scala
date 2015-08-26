package io.chronos.cluster.protocol

import io.chronos.cluster._
import io.chronos.id.TaskId

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  sealed trait WorkerMessage
  case class RegisterWorker(workerId: WorkerId) extends WorkerMessage
  case class RequestTask(workerId: WorkerId) extends WorkerMessage
  case class TaskDone(workerId: WorkerId, taskId: TaskId, result: Any) extends WorkerMessage
  case class TaskFailed(workerId: WorkerId, taskId: TaskId, cause: TaskFailureCause) extends WorkerMessage

  // Messages to workers
  case object TaskReady
  case class TaskDoneAck(taskId: TaskId)

  // Worker related events
  final val WorkerTopic = "Workers"

  sealed trait WorkerEvent
  case class WorkerJoined(workerId: WorkerId) extends WorkerEvent
  case class WorkerRemoved(workerId: WorkerId) extends WorkerEvent
}
