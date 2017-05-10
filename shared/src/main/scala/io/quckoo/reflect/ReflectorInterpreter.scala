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

import cats.{Monad, ~>}

/**
  * Created by alonsodomin on 04/05/2017.
  */
class ReflectorInterpreter[F[_]: Monad](impl: Reflector[F]) extends (ReflectOp ~> F) {

  override def apply[A](fa: ReflectOp[A]): F[A] = fa match {
    case ReflectOp.LoadJobClass(artifact, className) => impl.loadJobClass(artifact, className)
    case ReflectOp.CreateJob(jobClass) => impl.createJob(jobClass)
    case ReflectOp.RunJob(job) => impl.runJob(job)
  }

}

object ReflectorInterpreter {

  implicit def deriveInterpreter[F[_]: Monad](implicit reflector: Reflector[F]): ReflectorInterpreter[F] =
    new ReflectorInterpreter[F](reflector)

}
