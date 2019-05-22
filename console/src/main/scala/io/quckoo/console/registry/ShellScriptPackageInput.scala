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

import io.quckoo.ShellScriptPackage
import io.quckoo.console.components._
import io.quckoo.console.layout.lookAndFeel

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

object ShellScriptPackageInput {
  @inline private def lnf = lookAndFeel

  final val DefaultScript =
    """#!/bin/bash
      |
      |echo "Hello World"""".stripMargin

  final val EditorOptions = CodeEditor.Options(
    mode = CodeEditor.Mode.Shell,
    lineNumbers = true,
    matchBrackets = true,
    theme = Set(CodeEditor.Theme.Solarized, CodeEditor.Theme.Light)
  )

  type OnUpdate = Option[ShellScriptPackage] => Callback

  case class Props(value: Option[ShellScriptPackage], readOnly: Boolean, onUpdate: OnUpdate)
  case class State(content: Option[String])

  implicit val propsReuse: Reusability[Props] =
    Reusability.caseClassExcept('onUpdate)
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]

  class Backend($ : BackendScope[Props, State]) {

    private[this] def propagateChange: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.content.map(ShellScriptPackage.apply))))

    def onContentUpdate(value: Option[String]): Callback =
      $.modState(_.copy(content = value), propagateChange)

    def render(props: Props, state: State) = {
      def opts =
        if (props.readOnly) EditorOptions.copy(readOnly = "nocursor")
        else EditorOptions

      <.div(
        lnf.formGroup,
        <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "script_content", "Script"),
        <.div(
          ^.`class` := "col-sm-10",
          CodeEditor(
            state.content,
            onContentUpdate _,
            opts,
            ^.id := "script_content",
            ^.height := "250"
          )
        )
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]("ShellScriptPackageInput")
    .initialStateFromProps(props => State(props.value.map(_.content).orElse(Some(DefaultScript))))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[ShellScriptPackage], onUpdate: OnUpdate, readOnly: Boolean = false) =
    component(Props(value, readOnly, onUpdate))

}
