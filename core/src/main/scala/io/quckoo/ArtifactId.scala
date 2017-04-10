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

import cats._

import io.quckoo.validation._

import monocle.macros.Lenses

/**
  * Created by aalonsodominguez on 15/07/15.
  */
object ArtifactId {

  final val GroupSeparator: Char   = ':'
  final val VersionSeparator: Char = '#'

  val valid: Validator[ArtifactId] = {
    import Validators._

    val validOrganization = nonEmpty[String].at("organization")
    val validName         = nonEmpty[String].at("name")
    val validVersion      = nonEmpty[String].at("version")

    caseClass3(validOrganization, validName, validVersion)(ArtifactId.unapply, ArtifactId.apply)
  }

  implicit val artifactIdShow: Show[ArtifactId] = Show.show { artifactId =>
    s"${artifactId.organization}$GroupSeparator${artifactId.name}$VersionSeparator${artifactId.version}"
  }

  implicit val artifactIdEq: Eq[ArtifactId] = Eq.fromUniversalEquals[ArtifactId]

}

@Lenses final case class ArtifactId(organization: String, name: String, version: String)
