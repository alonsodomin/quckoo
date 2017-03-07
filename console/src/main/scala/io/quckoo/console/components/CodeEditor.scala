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

import io.quckoo.console.libs.codemirror._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.ReactStyle
import japgolly.scalajs.react.vdom.prefix_<^._

import enumeratum._

import org.scalajs.dom.Event
import scala.scalajs.js

/**
  * Created by alonsodomin on 02/03/2017.
  */
object CodeEditor {
  import CodeMirrorReact._

  type OnUpdate = Option[String] => Callback

  sealed trait Mode extends EnumEntry with EnumEntry.Lowercase
  object Mode extends Enum[Mode] {
    case object Scala extends Mode
    case object Shell extends Mode
    case object Python extends Mode

    val values = findValues
  }
  implicit val modeReuse: Reusability[Mode] = Reusability.byRef

  sealed trait Theme extends EnumEntry with EnumEntry.Lowercase
  object Theme extends Enum[Theme] {
    case object Monokai extends Theme
    case object Solarized extends Theme
    case object Dark extends Theme
    case object Light extends Theme

    val values = findValues
  }
  implicit val themeStyleReuse: Reusability[Theme] = Reusability.byRef

  case class Options(
    mode: Mode = Mode.Scala,
    lineNumbers: Boolean = false,
    lineSeparator: String = "\n",
    matchBrackets: Boolean = false,
    theme: Set[Theme] = Set.empty,
    tabSize: Int = 2,
    autoRefresh: Boolean = true,
    readOnly: ReadOnly = false
  )
  implicit val optionsReuse: Reusability[Options] = Reusability.caseClass[Options]

  final val DefaultWidth: Width = "100%"
  final val DefaultHeight: Height = 250

  case class Props(
    text: Option[String],
    onUpdate: OnUpdate,
    width: Width,
    height: Height,
    options: Options,
    attrs: Seq[TagMod]
  )
  case class State(value: Option[String])

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept('onUpdate, 'attrs)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {

    private[this] def propagateUpdate: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.value)))

    private[this] def jsOptions(props: Props): js.Dynamic = {
      js.Dynamic.literal(
        "mode"          -> props.options.mode.entryName,
        "lineNumbers"   -> props.options.lineNumbers,
        "lineSeparator" -> props.options.lineSeparator,
        "matchBrackets" -> props.options.matchBrackets,
        "theme"         -> props.options.theme.map(_.entryName).mkString(" "),
        "tabSize"       -> props.options.tabSize,
        "inputStyle"    -> "contenteditable",
        "autoRefresh"   -> props.options.autoRefresh,
        "readOnly"      -> props.options.readOnly.asInstanceOf[js.Any]
      )
    }

    protected[CodeEditor] def initialize(props: Props, state: State): Callback = Callback {
      val codeMirror = CodeMirror($.getDOMNode(), jsOptions(props))

      codeMirror.on("change", (cm, event) => onChange(cm, event.asInstanceOf[ChangeEvent]))
      codeMirror.on("blur", (cm, event) => onBlur(cm, event.asInstanceOf[Event]))

      codeMirror.setValue(props.text.getOrElse(""))
      codeMirror.setSize(props.width, props.height)

      codeMirror.refresh()
      codeMirror.markClean()
    }

    private[this] def valueUpdated(editorValue: Option[String]): Unit = {
      $.modState(_.copy(value = editorValue), propagateUpdate).runNow()
    }

    private[this] def onBlur(codeMirror: CodeMirror, event: Event): Unit = {
      val editorValue = Option(codeMirror.getValue()).filterNot(_.isEmpty)
      valueUpdated(editorValue)
    }

    private[this] def onChange(codeMirror: CodeMirror, change: ChangeEvent): Unit = {
      val editorValue = Option(codeMirror.getValue()).filterNot(_.isEmpty)
      if (!editorValue.contains(change.removed.mkString("\n")))
        valueUpdated(editorValue)
    }

    def render(props: Props, state: State) =
      <.div(props.attrs)

  }

  val component = ReactComponentB[Props]("CodeEditor")
    .initialState_P(props => State(props.text))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.initialize($.props, $.state))
    .build

  private[this] def extractWidthAndHeight(attrs: Seq[TagMod]): (Width, Height, List[TagMod]) = {
    import ReactStyle.NameAndValue

    val initial: (Width, Height, List[TagMod]) = (DefaultWidth, DefaultHeight, List.empty[TagMod])
    attrs.foldRight(initial) { case (attr, (w, h, acc)) =>
      attr match {
        case nv: NameAndValue[_] if nv.name == "width" =>
          (nv.value.asInstanceOf[Width], h, acc)
        case nv: NameAndValue[_] if nv.name == "height" =>
          (w, nv.value.asInstanceOf[Height], acc)
        case _ =>
          (w, h, attr :: acc)
      }
    }
  }

  def apply(value: Option[String], onUpdate: OnUpdate, attrs: TagMod*) = {
    val (width, height, remaining) = extractWidthAndHeight(attrs)
    component(Props(value, onUpdate, width, height, Options(), remaining))
  }

  def apply(value: Option[String], onUpdate: OnUpdate, options: Options, attrs: TagMod*) = {
    val (width, height, remaining) = extractWidthAndHeight(attrs)
    component(Props(value, onUpdate, width, height, options, remaining))
  }

}
