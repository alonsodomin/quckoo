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

package io.quckoo

import cats._
import cats.data.Validated
import cats.implicits._

import io.quckoo.md5.MD5
import io.quckoo.validation._

import monocle.Prism
import monocle.macros.{GenPrism, Lenses}

/**
  * Created by alonsodomin on 17/02/2017.
  */
sealed trait JobPackage {
  def checksum: String
}

object JobPackage {

  val valid: Validator[JobPackage] = {
    (ShellScriptPackage.valid <*> JarJobPackage.valid).dimap[JobPackage, Validated[Violation, JobPackage]](
      {
        case shell: ShellScriptPackage => Left(shell)
        case jar: JarJobPackage        => Right(jar)
      })(
      _.map(_.fold(_.asInstanceOf[JobPackage], _.asInstanceOf[JobPackage]))
    )
  }

  def jar(artifactId: ArtifactId, jobClass: String): JarJobPackage =
    JarJobPackage(artifactId, jobClass)

  def shell(content: String): ShellScriptPackage = ShellScriptPackage(content)

  val asJar: Prism[JobPackage, JarJobPackage] = GenPrism[JobPackage, JarJobPackage]
  val asShell: Prism[JobPackage, ShellScriptPackage] = GenPrism[JobPackage, ShellScriptPackage]

  implicit val jobPackageShow: Show[JobPackage] = Show.show {
    case jar: JarJobPackage        => jar.show
    case shell: ShellScriptPackage => shell.show
  }

}

@Lenses final case class ShellScriptPackage(content: String) extends JobPackage {
  override def checksum: String = MD5.checksum(content)
}
object ShellScriptPackage {

  implicit val shellScriptPackageShow: Show[ShellScriptPackage] =
    Show.fromToString

  val valid: Validator[ShellScriptPackage] = {
    import Validators._

    val validContent = nonEmpty[String].at("content")
    caseClass1(validContent)(ShellScriptPackage.unapply, ShellScriptPackage.apply)
  }

}

@Lenses final case class JarJobPackage(
  artifactId: ArtifactId,
  jobClass: String
) extends JobPackage {

  def checksum: String = MD5.checksum(s"$artifactId!$jobClass")

}

object JarJobPackage {

  implicit val jobPackageShow: Show[JarJobPackage] = Show.show { pckg =>
    s"${pckg.jobClass} @ ${pckg.artifactId.show}"
  }

  val valid: Validator[JarJobPackage] = {
    import Validators._

    val validArtifactId = ArtifactId.valid.at("artifactId")
    val validJobClass   = nonEmpty[String].at("jobClass")

    caseClass2(validArtifactId, validJobClass)(JarJobPackage.unapply, JarJobPackage.apply)
  }

}
