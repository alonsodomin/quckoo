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

package io.quckoo.console.registry

import io.quckoo.JarJobPackage
import io.quckoo.id.ArtifactId
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 15/02/2017.
  */
object JarJobPackageInput {
  @inline private def lnf = lookAndFeel

  type OnUpdate = Option[JarJobPackage] => Callback

  case class Props(value: Option[JarJobPackage], onUpdate: OnUpdate)
  case class State(artifactId: Option[ArtifactId], jobClass: Option[String]) {

    def this(value: Option[JarJobPackage]) =
      this(value.map(_.artifactId), value.map(_.jobClass))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {
    private[this] val jobClassInput = Input[String]()

    private[this] def propagateChange: Callback = {
      val jarPackage = for {
        artifactId <- $.state.map(_.artifactId).asCBO[ArtifactId]
        jobClass   <- $.state.map(_.jobClass).asCBO[String]
      } yield JarJobPackage(artifactId, jobClass)

      jarPackage.get.flatMap(value => $.props.flatMap(_.onUpdate(value)))
    }

    def onArtifactIdUpdate(value: Option[ArtifactId]): Callback =
      $.modState(_.copy(artifactId = value), propagateChange)

    def onJobClassUpdate(value: Option[String]): Callback =
      $.modState(_.copy(jobClass = value), propagateChange)

    def render(props: Props, state: State) = {
      <.div(
        <.div(lnf.formGroup,
          <.label("Artifact"),
          ArtifactInput(state.artifactId, onArtifactIdUpdate)
        ),
        <.div(lnf.formGroup,
          <.label(^.`for` := "jobClass", "Job Class"),
          jobClassInput(state.jobClass,
            onJobClassUpdate _,
            ^.id := "jobClass"
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("JarJobPackageInput")
    .initialState_P(props => new State(props.value))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[JarJobPackage], onUpdate: OnUpdate) =
    component(Props(value, onUpdate))

}
