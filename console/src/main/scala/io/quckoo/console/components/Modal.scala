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

package io.quckoo.console.components

import io.quckoo.console.libs._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import org.scalajs.jquery._

import scala.scalajs.js
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Modal {

  case class Options(backdrop: Boolean = true, keyboard: Boolean = true, show: Boolean = true)

  case class Props(header: Callback => ReactNode,
                   footer: Callback => ReactNode,
                   onClosed: Callback,
                   onShown: Option[Callback] = None,
                   options: Options = Options())

  class Backend($ : BackendScope[Props, Unit]) {

    // Initialization code

    private[Modal] def initialize(props: Props): Callback = Callback {
      // instruct Bootstrap to show the modal
      // $COVERAGE-OFF$ https://github.com/scoverage/scalac-scoverage-plugin/issues/176
      jQuery($.getDOMNode()).modal(
        js.Dynamic.literal(
          "backdrop" -> props.options.backdrop,
          "keyboard" -> props.options.keyboard,
          "show"     -> props.options.show
        )
      )
      // $COVERAGE-ON$

      // register event listener to be notified when the modal is closed
      jQuery($.getDOMNode()).on("hidden.bs.modal", null, null, onHidden _)
      jQuery($.getDOMNode()).on("shown.bs.modal", null, null, onShown _)
    }

    private[this] def onHidden(e: JQueryEventObject): js.Any =
      $.props.flatMap(_.onClosed).runNow()

    private[this] def onShown(e: JQueryEventObject): js.Any =
      $.props.flatMap(_.onShown.getOrElse(Callback.empty)).runNow()

    // Actions

    private def invokeCmd(cmd: String): Callback = Callback {
      jQuery($.getDOMNode()).modal(cmd)
    }

    def toggle() : Callback = invokeCmd("toggle")
    def show()   : Callback = invokeCmd("show")
    def hide()   : Callback = invokeCmd("hide")

    // Rendering

    def render(props: Props, children: PropsChildren) = {
      val modalStyle = lookAndFeel.modal
      <.div(
        modalStyle.modal,
        modalStyle.fade,
        ^.role := "dialog",
        ^.aria.hidden := true,
        <.div(
          modalStyle.dialog,
          <.div(
            modalStyle.content,
            <.div(modalStyle.header, props.header(hide())),
            <.div(modalStyle.body, children),
            <.div(modalStyle.footer, props.footer(hide())))))
    }
  }

  val component = ReactComponentB[Props]("Modal")
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.initialize($.props))
    .build

  def apply()                                   = component
  def apply(props: Props, children: ReactNode*) = component(props, children: _*)
}
