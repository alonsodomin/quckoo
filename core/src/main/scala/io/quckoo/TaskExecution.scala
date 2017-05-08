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

import cats.Show

import monocle.macros.Lenses

/**
  * Created by alonsodomin on 24/07/2016.
  */
@Lenses final case class TaskExecution(
    planId: PlanId,
    task: Task,
    status: TaskExecution.Status,
    outcome: Option[TaskExecution.Outcome] = None
)

object TaskExecution {

  sealed trait Status extends Product with Serializable
  object Status {
    case object Scheduled  extends Status
    case object Enqueued   extends Status
    case object InProgress extends Status
    case object Complete   extends Status
  }

  implicit val statusShow: Show[Status] = Show.fromToString

  sealed trait Reason extends Product with Serializable
  object Reason {
    case object UserRequest     extends Reason
    case object FailedToEnqueue extends Reason
  }

  implicit val reasonShow: Show[Reason] = Show.fromToString

  sealed trait Outcome extends Product with Serializable
  object Outcome {
    case object Success                          extends Outcome
    final case class Failure(cause: QuckooError)       extends Outcome
    final case class Interrupted(reason: Reason) extends Outcome
    final case class NeverRun(reason: Reason)    extends Outcome
    case object NeverEnding                      extends Outcome
  }

  implicit val outcomeShow: Show[Outcome] = Show.fromToString

}
