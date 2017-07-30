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

import cats.effect.IO
import cats.syntax.eq._
import cats.syntax.validated._

import io.quckoo.reflect.Artifact
import io.quckoo.{ArtifactId, DependencyError, UnresolvedDependency}

/**
  * Created by alonsodomin on 06/05/2017.
  */
class PureResolver(artifact: Option[Artifact] = None) extends Resolver[IO] {

  def this(artifact: Artifact) = this(Some(artifact))

  override def validate(artifactId: ArtifactId): IO[Resolved[ArtifactId]] = IO {
    if (artifact.exists(_.artifactId === artifactId))
      artifactId.validNel[DependencyError]
    else UnresolvedDependency(artifactId).invalidNel[ArtifactId]
  }

  override def download(artifactId: ArtifactId): IO[Resolved[Artifact]] = IO {
    artifact
      .filter(_.artifactId === artifactId)
      .map(_.validNel[DependencyError])
      .getOrElse(UnresolvedDependency(artifactId).invalidNel[Artifact])
  }

}
