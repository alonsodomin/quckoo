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

import io.quckoo.fault._
import io.quckoo.id._

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

  sealed trait UncompleteReason
  case object UserRequest extends UncompleteReason
  case object FailedToEnqueue extends UncompleteReason

  sealed trait Outcome extends Serializable
  case object NotStarted extends Outcome
  case object Success extends Outcome
  case class Failure(cause: Fault) extends Outcome
  case class Interrupted(reason: UncompleteReason) extends Outcome
  case class NeverRun(reason: UncompleteReason) extends Outcome
  case object NeverEnding extends Outcome

}
