package io.quckoo.cluster.protocol

import io.quckoo.cluster._
import io.quckoo.fault.Faults
import io.quckoo.id.TaskId

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  sealed trait WorkerMessage
  case class RegisterWorker(workerId: WorkerId) extends WorkerMessage
  case class RequestTask(workerId: WorkerId) extends WorkerMessage
  case class TaskDone(workerId: WorkerId, taskId: TaskId, result: Any) extends WorkerMessage
  case class TaskFailed(workerId: WorkerId, taskId: TaskId, cause: Faults) extends WorkerMessage

  // Messages to workers
  sealed trait MasterMessage
  case object TaskReady extends MasterMessage
  case class TaskDoneAck(taskId: TaskId) extends MasterMessage

  // Worker related events
  final val WorkerTopic = "Workers"

  sealed trait WorkerEvent
  case class WorkerJoined(workerId: WorkerId) extends WorkerEvent
  case class WorkerRemoved(workerId: WorkerId) extends WorkerEvent
}
