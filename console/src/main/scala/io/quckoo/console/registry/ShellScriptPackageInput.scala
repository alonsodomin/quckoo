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

import io.quckoo.ShellScriptPackage
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.ScalaCssReact._

object ShellScriptPackageInput {
  @inline private def lnf = lookAndFeel

  final val DefaultScript =
    """#!/bin/bash
      |
      |echo "Hello World"""".stripMargin

  final val EditorOptions = CodeEditor.Options(
    mode = Some(CodeEditor.Mode.Shell),
    lineNumbers = true,
    theme = Set(CodeEditor.ThemeStyle.Solarized, CodeEditor.ThemeStyle.Light)
  )

  type OnUpdate = Option[ShellScriptPackage] => Callback

  case class Props(value: Option[ShellScriptPackage], onUpdate: OnUpdate)
  case class State(content: Option[String])

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {

    private[this] def propagateChange: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.content.map(ShellScriptPackage.apply))))

    def onContentUpdate(value: Option[String]): Callback =
      $.modState(_.copy(content = value), propagateChange)

    def render(props: Props, state: State) = {
      <.div(lnf.formGroup,
        <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "script_content", "Script"),
        <.div(^.`class` := "col-sm-10",
          CodeEditor(state.content,
            onContentUpdate _,
            EditorOptions,
            ^.id := "script_content",
            ^.height := 250
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("ShellScriptPackageInput")
    .initialState_P(props => State(props.value.map(_.content).orElse(Some(DefaultScript))))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[ShellScriptPackage], onUpdate: OnUpdate) =
    component(Props(value, onUpdate))

}
