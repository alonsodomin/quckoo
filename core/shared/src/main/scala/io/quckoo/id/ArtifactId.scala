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

package io.quckoo.id

import io.quckoo.fault._
import io.quckoo.validation._

import monocle.macros.Lenses

import scalaz._

/**
 * Created by aalonsodominguez on 15/07/15.
 */
object ArtifactId {
  import Scalaz._

  final val GroupSeparator : Char = ':'
  final val VersionSeparator : Char = '#'

  def validate(artifactId: ArtifactId): ValidationNel[ValidationFault, ArtifactId] =
    validate(artifactId.organization, artifactId.name, artifactId.version)

  def validate(organization: String, name: String, version: String): ValidationNel[ValidationFault, ArtifactId] = {
    import Validations._

    def validGroup: Validation[ValidationFault, String] =
      notNullOrEmpty(organization)("organization")

    def validArtifactId: Validation[ValidationFault, String] =
      notNullOrEmpty(name)("name")

    def validVersion: Validation[ValidationFault, String] =
      notNullOrEmpty(version)("version")

    (validGroup.toValidationNel |@| validArtifactId.toValidationNel |@| validVersion.toValidationNel)(ArtifactId.apply)
  }

  implicit val instance = new Equal[ArtifactId] with Show[ArtifactId] {

    override def equal(left: ArtifactId, right: ArtifactId): Boolean =
      left.organization == right.organization &&
        left.name == right.name &&
        left.version == right.version

  }

}

@Lenses final case class ArtifactId(organization: String, name: String, version: String) {
  import ArtifactId._

  override def toString: String = s"$organization$GroupSeparator$name$VersionSeparator$version"

}
