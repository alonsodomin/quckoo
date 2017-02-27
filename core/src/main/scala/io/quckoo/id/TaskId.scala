/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.id

import java.util.UUID

import upickle.default.{Reader => UReader, Writer => UWriter, _}

/**
  * Created by alonsodomin on 27/02/2017.
  */
final class TaskId private (private val uuid: UUID) extends AnyVal {

  override def toString: String = uuid.toString

}

object TaskId {

  @inline def apply(uuid: UUID): TaskId = new TaskId(uuid)
  @inline def apply(value: String): TaskId = new TaskId(UUID.fromString(value))

  implicit val upickleReader: UReader[TaskId] = UReader[TaskId] {
    implicitly[UReader[UUID]].read andThen TaskId.apply
  }

  implicit val upickleWriter: UWriter[TaskId] = UWriter[TaskId] { taskId =>
    implicitly[UWriter[UUID]].write(taskId.uuid)
  }

}