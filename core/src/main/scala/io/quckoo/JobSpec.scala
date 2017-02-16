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

import io.quckoo.id._
import io.quckoo.validation._

import monocle.macros.Lenses

import scalaz.Show
import scalaz.syntax.show._

/**
  * Created by aalonsodominguez on 10/07/15.
  */
@Lenses final case class JobSpec(
    displayName: String,
    description: Option[String] = None,
    jobPackage: JobPackage,
    disabled: Boolean = false
)

object JobSpec {

  val valid: Validator[JobSpec] = {
    import Validators._

    val validDisplayName = nonEmpty[String].at("displayName")
    val validDetails = JobPackage.valid.at("jobPackage")

    caseClass4(validDisplayName,
      any[Option[String]],
      validDetails,
      any[Boolean])(JobSpec.unapply, JobSpec.apply)
  }

  implicit val display: Show[JobSpec] = Show.showFromToString[JobSpec]

}

sealed trait JobPackage {
  def hash: String
}

object JobPackage {

  val valid: Validator[JobPackage] = JarJobPackage.valid.dimap({
    case jar: JarJobPackage => jar
  }, _.map(_.asInstanceOf[JobPackage]))

  def jar(artifactId: ArtifactId, jobClass: String) =
    JarJobPackage(artifactId, jobClass)

  implicit val jobPackageShow: Show[JobPackage] = Show.shows {
    case jar: JarJobPackage => Show[JarJobPackage].shows(jar)
  }

}

@Lenses final case class JarJobPackage(
    artifactId: ArtifactId,
    jobClass: String
) extends JobPackage {

  // TODO replace by a proper implementation
  def hash: String = s"$artifactId!$jobClass"

}

object JarJobPackage {

  implicit val jobPackageShow: Show[JarJobPackage] = Show.shows { pckg =>
    s"${pckg.jobClass} @ ${pckg.artifactId.shows}"
  }

  val valid: Validator[JarJobPackage] = {
    import Validators._

    val validArtifactId = ArtifactId.valid.at("artifactId")
    val validJobClass   = nonEmpty[String].at("jobClass")

    caseClass2(validArtifactId, validJobClass)(JarJobPackage.unapply, JarJobPackage.apply)
  }

}
