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

package io.quckoo

import io.quckoo.id._

import scalaz._
import Scalaz._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

final case class Task(
    id: TaskId,
    artifactId: ArtifactId,
    //params: Map[String, AnyVal] = Map.empty,
    jobClass: String
)

object Task {

  implicit val showTask = Show.shows[Task] { task =>
    s"${task.jobClass} @ ${task.artifactId.shows}"
  }

}
