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

import io.quckoo.id.ArtifactId
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 03/07/2016.
  */
object ArtifactInput {

  case class Props(value: Option[ArtifactId], onUpdate: Option[ArtifactId] => Callback)
  case class State(groupId: Option[String], artifactId: Option[String], version: Option[String]) {

    def this(value: Option[ArtifactId]) =
      this(value.map(_.group), value.map(_.artifact), value.map(_.version))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val artifactId = for {
        group   <- $.state.map(_.groupId).asCBO[String]
        name    <- $.state.map(_.artifactId).asCBO[String]
        version <- $.state.map(_.version).asCBO[String]
      } yield ArtifactId(group, name, version)

      artifactId.get.flatMap(value => $.props.flatMap(_.onUpdate(value)))
    }

    def onGroupUpdate(group: Option[String]): Callback =
      $.modState(_.copy(groupId = group), propagateUpdate)

    def onNameUpdate(name: Option[String]): Callback =
      $.modState(_.copy(artifactId = name), propagateUpdate)

    def onVersionUpdate(version: Option[String]): Callback =
      $.modState(_.copy(version = version), propagateUpdate)

    def render(props: Props, state: State) = {
      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-4",
            Input(state.groupId, onGroupUpdate _, ^.id := "artifactGroup", ^.placeholder := "Group")
          ),
          <.div(^.`class` := "col-sm-4",
            Input(state.artifactId, onNameUpdate _, ^.id := "artifactName", ^.placeholder := "Name")
          ),
          <.div(^.`class` := "col-sm-4",
            Input(state.version, onVersionUpdate _, ^.id := "artifactVerion", ^.placeholder := "Version")
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("ArtifactInput").
    initialState_P(props => new State(props.value)).
    renderBackend[Backend].
    configure(Reusability.shouldComponentUpdate).
    build

  def apply(value: Option[ArtifactId], onUpdate: Option[ArtifactId] => Callback) =
    component(Props(value, onUpdate))

}
