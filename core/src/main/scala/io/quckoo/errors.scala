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

package io.quckoo

import cats.data.NonEmptyList

import io.quckoo.validation.Violation

/**
  * Created by alonsodomin on 28/12/2015.
  */
sealed trait QuckooError extends Product with Serializable

// == Business errors ===============

sealed trait InvalidJob extends QuckooError {
  def jobId: JobId
}
final case class JobNotFound(jobId: JobId)             extends InvalidJob
final case class JobNotEnabled(jobId: JobId)           extends InvalidJob
final case class ExecutionPlanNotFound(planId: PlanId) extends QuckooError
final case class TaskExecutionNotFound(taskId: TaskId) extends QuckooError

// == Generic errors ================

final case class ExceptionThrown(className: String, message: String) extends QuckooError {

  override def toString: String = s"$className: $message"

}
object ExceptionThrown {
  def from(t: Throwable): ExceptionThrown = ExceptionThrown(t.getClass.getName, t.getMessage)
}

// == Artifact resolution errors ============

final case class MissingDependencies(dependencies: NonEmptyList[DependencyError]) extends QuckooError

sealed trait DependencyError extends Product with Serializable {
  val artifactId: ArtifactId
}
case class UnresolvedDependency(artifactId: ArtifactId) extends DependencyError

object DownloadFailed {
  sealed trait Reason
  case object NotFound                    extends Reason
  final case class Other(message: String) extends Reason
}
final case class DownloadFailed(artifactId: ArtifactId, reason: DownloadFailed.Reason)
    extends DependencyError

// == Validation errors ====================

final case class ValidationFault(violation: Violation) extends QuckooError

// == Task Runtime Errors ==================

final case class TaskExitCodeFault(exitCode: Int) extends QuckooError
