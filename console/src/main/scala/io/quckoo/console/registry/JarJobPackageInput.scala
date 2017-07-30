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

package io.quckoo.console.registry

import io.quckoo.{ArtifactId, JarJobPackage}
import io.quckoo.console.components._
import io.quckoo.console.layout._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 15/02/2017.
  */
object JarJobPackageInput {
  @inline private def lnf = lookAndFeel

  type OnUpdate = Option[JarJobPackage] => Callback

  case class Props(value: Option[JarJobPackage],
                   readOnly: Boolean,
                   onUpdate: OnUpdate)
  case class State(artifactId: Option[ArtifactId], jobClass: Option[String]) {

    def this(value: Option[JarJobPackage]) =
      this(value.map(_.artifactId), value.map(_.jobClass))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass[State]

  class Backend($ : BackendScope[Props, State]) {

    private[this] def propagateChange: Callback = {
      val jarPackage = for {
        artifactId <- $.state.map(_.artifactId).asCBO[ArtifactId]
        jobClass <- $.state.map(_.jobClass).asCBO[String]
      } yield JarJobPackage(artifactId, jobClass)

      jarPackage.asCallback.flatMap(value => $.props.flatMap(_.onUpdate(value)))
    }

    def onArtifactIdUpdate(value: Option[ArtifactId]): Callback =
      $.modState(_.copy(artifactId = value), propagateChange)

    def onJobClassUpdate(value: Option[String]): Callback =
      $.modState(_.copy(jobClass = value), propagateChange)

    private[this] val JobClassInput = Input[String]

    def render(props: Props, state: State) = {
      <.div(
        <.div(
          lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label", "Artifact"),
          <.div(
            ^.`class` := "col-sm-10",
            ArtifactInput(state.artifactId, onArtifactIdUpdate, props.readOnly))
        ),
        <.div(
          lnf.formGroup,
          <.label(^.`class` := "col-sm-2 control-label",
                  ^.`for` := "jobClass",
                  "Job Class"),
          <.div(^.`class` := "col-sm-10",
                JobClassInput(state.jobClass,
                              onJobClassUpdate _,
                              ^.id := "jobClass",
                              ^.placeholder := "Job main class",
                              ^.readOnly := props.readOnly))
        )
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]("JarJobPackageInput")
    .initialStateFromProps(props => new State(props.value))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[JarJobPackage],
            onUpdate: OnUpdate,
            readOnly: Boolean = false) =
    component(Props(value, readOnly, onUpdate))

}
