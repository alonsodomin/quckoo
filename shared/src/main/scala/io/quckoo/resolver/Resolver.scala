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

package io.quckoo.resolver

import cats.free.{Free, Inject}

import io.quckoo.ArtifactId
import io.quckoo.reflect.Artifact

/**
  * Created by alonsodomin on 03/05/2017.
  */
trait Resolver[F[_]] {

  def validate(artifactId: ArtifactId): F[Resolved[ArtifactId]]

  def download(artifactId: ArtifactId): F[Resolved[Artifact]]

}

class InjectableResolver[F[_]](implicit inject: Inject[ResolverOp, F]) extends Resolver[Free[F, ?]] {

  override def validate(artifactId: ArtifactId): Free[F, Resolved[ArtifactId]] =
    Free.inject[ResolverOp, F](ResolverOp.Validate(artifactId))

  override def download(artifactId: ArtifactId): Free[F, Resolved[Artifact]] =
    Free.inject[ResolverOp, F](ResolverOp.Download(artifactId))

}

object InjectableResolver {

  implicit def injectableResolver[F[_]](implicit inject: Inject[ResolverOp, F]): InjectableResolver[F] =
    new InjectableResolver[F]

}
