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

import io.quckoo.ArtifactId
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 03/07/2016.
  */
object ArtifactInput {

  type OnUpdate = Option[ArtifactId] => Callback

  case class Props(value: Option[ArtifactId], readOnly: Boolean, onUpdate: OnUpdate)
  case class State(organization: Option[String], name: Option[String], version: Option[String]) {

    def this(value: Option[ArtifactId]) =
      this(value.map(_.organization), value.map(_.name), value.map(_.version))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse                     = Reusability.caseClass[State]

  class Backend($ : BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val artifactId = for {
        organization <- $.state.map(_.organization).asCBO[String]
        name         <- $.state.map(_.name).asCBO[String]
        version      <- $.state.map(_.version).asCBO[String]
      } yield ArtifactId(organization, name, version)

      artifactId.get.flatMap(value => $.props.flatMap(_.onUpdate(value)))
    }

    def onGroupUpdate(organization: Option[String]): Callback =
      $.modState(_.copy(organization = organization), propagateUpdate)

    def onNameUpdate(name: Option[String]): Callback =
      $.modState(_.copy(name = name), propagateUpdate)

    def onVersionUpdate(version: Option[String]): Callback =
      $.modState(_.copy(version = version), propagateUpdate)

    val organizationInput = Input[String]()
    val nameInput         = Input[String]()
    val versionInput      = Input[String]()

    def render(props: Props, state: State) = {
      <.div(
        ^.`class` := "container-fluid",
        <.div(
          ^.`class` := "row",
          <.div(
            ^.`class` := "col-sm-4",
            organizationInput(
              state.organization,
              onGroupUpdate _,
              ^.id := "artifactOrganization",
              ^.placeholder := "Organization",
              ^.readOnly := props.readOnly
            )),
          <.div(
            ^.`class` := "col-sm-4",
            nameInput(
              state.name,
              onNameUpdate _,
              ^.id := "artifactName",
              ^.placeholder := "Name",
              ^.readOnly := props.readOnly
            )),
          <.div(
            ^.`class` := "col-sm-4",
            versionInput(
              state.version,
              onVersionUpdate _,
              ^.id := "artifactVerion",
              ^.placeholder := "Version",
              ^.readOnly := props.readOnly
            ))))
    }

  }

  val component = ReactComponentB[Props]("ArtifactInput")
    .initialState_P(props => new State(props.value))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[ArtifactId], onUpdate: OnUpdate, readOnly: Boolean = false) =
    component(Props(value, readOnly, onUpdate))

}
