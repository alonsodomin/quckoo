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

package io.quckoo.reflect

import cats.InjectK
import cats.free.Free

import io.quckoo.{Job, JobClass}

/**
  * Created by alonsodomin on 04/05/2017.
  */
trait Reflector[F[_]] {
  def loadJobClass(artifact: Artifact, className: String): F[JobClass]
  def createJob(jobClass: JobClass): F[Job]
  def runJob(job: Job): F[Unit]
}

class InjectableReflector[F[_]](implicit inject: InjectK[ReflectOp, F])
    extends Reflector[Free[F, ?]] {

  override def loadJobClass(artifact: Artifact,
                            className: String): Free[F, JobClass] =
    Free.inject[ReflectOp, F](ReflectOp.LoadJobClass(artifact, className))

  override def createJob(jobClass: JobClass): Free[F, Job] =
    Free.inject[ReflectOp, F](ReflectOp.CreateJob(jobClass))

  override def runJob(job: Job): Free[F, Unit] =
    Free.inject[ReflectOp, F](ReflectOp.RunJob(job))

}
object InjectableReflector {
  implicit def injectableReflector[F[_]](
      implicit inject: InjectK[ReflectOp, F]): InjectableReflector[F] =
    new InjectableReflector[F]
}
