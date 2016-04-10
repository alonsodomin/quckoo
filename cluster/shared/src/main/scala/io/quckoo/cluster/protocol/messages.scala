package io.quckoo.cluster.protocol

import io.quckoo.fault._
import io.quckoo.id._

// Messages from workers
sealed trait WorkerMessage
final case class RegisterWorker(workerId: NodeId) extends WorkerMessage
final case class RequestTask(workerId: NodeId) extends WorkerMessage
final case class TaskDone(workerId: NodeId, taskId: TaskId, result: Any) extends WorkerMessage
final case class TaskFailed(workerId: NodeId, taskId: TaskId, cause: Faults) extends WorkerMessage

// Messages to workers
sealed trait MasterMessage
case object TaskReady extends MasterMessage
final case class TaskDoneAck(taskId: TaskId) extends MasterMessage