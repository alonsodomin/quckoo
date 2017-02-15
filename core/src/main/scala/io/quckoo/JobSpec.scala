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

/**
  * Created by aalonsodominguez on 10/07/15.
  */
object JobSpec {

  val valid: Validator[JobSpec] = JarJobSpec.valid.dimap({
    case jar: JarJobSpec => jar
  }, _.map(_.asInstanceOf[JobSpec]))

  def jar(
    displayName: String,
    description: Option[String] = None,
    artifactId: ArtifactId,
    jobClass: String,
    disabled: Boolean = false
  ) = JarJobSpec(displayName, description, artifactId, jobClass, disabled)

  implicit val display: Show[JobSpec] = Show.showFromToString[JobSpec]

}

sealed trait JobSpec {
  def displayName: String
  def description: Option[String]
  def artifactId: ArtifactId
  def jobClass: String
  def disabled: Boolean

  def enable(): JobSpec
  def disable(): JobSpec
}

@Lenses case class JarJobSpec(
    displayName: String,
    description: Option[String] = None,
    artifactId: ArtifactId,
    jobClass: String,
    disabled: Boolean = false
) extends JobSpec {
  def enable() = copy(disabled = false)
  def disable() = copy(disabled = true)
}

object JarJobSpec {

  val valid: Validator[JarJobSpec] = {
    import Validators._

    val validDisplayName = nonEmpty[String].at("displayName")
    val validArtifactId  = ArtifactId.valid.at("artifactId")
    val validJobClass    = nonEmpty[String].at("jobClass")

    caseClass5(
      validDisplayName,
      any[Option[String]],
      validArtifactId,
      validJobClass,
      any[Boolean])(JarJobSpec.unapply, JarJobSpec.apply)
  }

  implicit val display: Show[JarJobSpec] = Show.showFromToString[JarJobSpec]
}
