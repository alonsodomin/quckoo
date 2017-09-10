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

package io.quckoo.console.core.remote

import io.quckoo.api2.Registry
import io.quckoo.console.core.ConsoleIO
import io.quckoo.{JobId, JobSpec}

private[remote] trait ConsoleRegistry extends Registry[ConsoleIO] {

  override def enableJob(jobId: JobId) =
    ConsoleIO(_.enableJob(jobId))

  override def disableJob(jobId: JobId) =
    ConsoleIO(_.disableJob(jobId))

  override def fetchJob(jobId: JobId) =
    ConsoleIO(_.fetchJob(jobId))

  override def allJobs =
    ConsoleIO(_.allJobs)

  override def registerJob(jobSpec: JobSpec) =
    ConsoleIO(_.registerJob(jobSpec))
}
