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

package io.quckoo.resolver

import cats.{Monad, ~>}

/**
  * Created by alonsodomin on 04/05/2017.
  */
final class ResolverInterpreter[F[_]: Monad] private (impl: Resolver[F]) extends (ResolverOp ~> F) {

  override def apply[A](fa: ResolverOp[A]): F[A] = fa match {
    case ResolverOp.Validate(artifactId) => impl.validate(artifactId)
    case ResolverOp.Download(artifactId) => impl.download(artifactId)
  }

}

object ResolverInterpreter {

  implicit def deriveInstance[F[_]: Monad](implicit impl: Resolver[F]): ResolverInterpreter[F] =
    new ResolverInterpreter[F](impl)

}
