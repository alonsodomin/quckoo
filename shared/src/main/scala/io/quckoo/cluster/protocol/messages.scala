/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.protocol

import io.quckoo.{QuckooError, NodeId, TaskId}

// Messages from workers
sealed trait WorkerMessage {
  def workerId: NodeId
}
final case class RegisterWorker(workerId: NodeId)                           extends WorkerMessage
final case class RemoveWorker(workerId: NodeId)                             extends WorkerMessage
final case class RequestTask(workerId: NodeId)                              extends WorkerMessage
final case class TaskDone(workerId: NodeId, taskId: TaskId, result: Any)    extends WorkerMessage
final case class TaskFailed(workerId: NodeId, taskId: TaskId, cause: QuckooError) extends WorkerMessage

// Messages to workers
sealed trait MasterMessage
case object TaskReady extends MasterMessage
final case class TaskDoneAck(taskId: TaskId) extends MasterMessage
