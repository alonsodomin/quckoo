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

package io.quckoo.client.registry

import cats.InjectK
import cats.free.Free
import io.quckoo.api2.Registry
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}
import io.quckoo.{JobId, JobNotFound, JobSpec}

class RegistryClientAPI[F[_]](implicit injectK: InjectK[RegistryOp, F])
    extends Registry[Free[F, ?]] {
  import RegistryOp._

  override def enableJob(jobId: JobId): Free[F, Either[JobNotFound, JobEnabled]] =
    Free.inject[RegistryOp, F](EnableJob(jobId))

  override def disableJob(jobId: JobId): Free[F, Either[JobNotFound, JobDisabled]] =
    Free.inject[RegistryOp, F](DisableJob(jobId))

  override def fetchJob(jobId: JobId): Free[F, Option[JobSpec]] =
    Free.inject[RegistryOp, F](FetchJob(jobId))

  override def registerJob(jobSpec: JobSpec) = ???

}

object RegistryClientAPI {
  implicit def registryClientAPI[F[_]](implicit injectK: InjectK[RegistryOp, F]): RegistryClientAPI[F] =
    new RegistryClientAPI[F]
}
