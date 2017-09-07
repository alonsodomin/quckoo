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

package io.quckoo.console.components

import io.quckoo.console.layout.lookAndFeel
import io.quckoo.console.libs._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import org.scalajs.jquery._

import scala.scalajs.js
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Modal {

  case class Options(backdrop: Boolean = true, keyboard: Boolean = true, show: Boolean = true)

  case class Props(header: Callback => VdomNode,
                   footer: Callback => VdomNode,
                   onClosed: Callback,
                   onShown: Option[Callback] = None,
                   options: Options = Options())

  class Backend($ : BackendScope[Props, Unit]) {
    private type RawModal = JQuery
    private type Listener = JQueryEventObject => js.Any

    // Initialization code

    private[Modal] def initialize(props: Props): Callback = {
      def initJS: CallbackTo[RawModal] = $.getDOMNode.map { node =>
        // instruct Bootstrap to show the modal
        // $COVERAGE-OFF$ https://github.com/scoverage/scalac-scoverage-plugin/issues/176
        jQuery(node).modal(
          js.Dynamic.literal(
            "backdrop" -> props.options.backdrop,
            "keyboard" -> props.options.keyboard,
            "show"     -> props.options.show
          )
        )
        // $COVERAGE-ON$
      }

      def registerListener(name: String, handler: Listener)(modal: RawModal): RawModal =
        modal.on(name, null, null, handler)

      val actions = initJS
        .map(registerListener("hidden.bs.modal", onHidden))
        .map(registerListener("shown.bs.modal", onShown))

      actions.void
    }

    private[this] def onHidden(e: JQueryEventObject): js.Any =
      $.props.flatMap(_.onClosed).runNow()

    private[this] def onShown(e: JQueryEventObject): js.Any =
      $.props.flatMap(_.onShown.getOrElse(Callback.empty)).runNow()

    // Actions

    private def invokeCmd(cmd: String): Callback =
      $.getDOMNode.map { node =>
        jQuery(node).modal(cmd)
      } void

    def toggle(): Callback = invokeCmd("toggle")
    def show(): Callback   = invokeCmd("show")
    def hide(): Callback   = invokeCmd("hide")

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
            <.div(modalStyle.footer, props.footer(hide()))
          )
        )
      )
    }
  }

  val Component = ScalaComponent
    .builder[Props]("Modal")
    .renderBackendWithChildren[Backend]
    .componentDidMount($ => $.backend.initialize($.props))
    .build

  def apply(header: Callback => VdomNode,
            footer: Callback => VdomNode,
            onClosed: Callback,
            onShown: Option[Callback] = None,
            options: Options = Options()) =
    Component(Props(header, footer, onClosed, onShown, options)) _

  def apply(props: Props, children: VdomNode*) = Component(props)(children: _*)
}
