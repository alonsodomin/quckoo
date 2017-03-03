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
import japgolly.scalajs.react.vdom.ReactAttr
import japgolly.scalajs.react.vdom.prefix_<^._

import enumeratum._

import org.scalajs.dom.html

import scala.scalajs.js

/**
  * Created by alonsodomin on 02/03/2017.
  */
object CodeEditor {
  import CodeMirrorReact._
  import ReactAttr.NameAndValue

  type OnUpdate = Option[String] => Callback

  sealed trait Mode extends EnumEntry with EnumEntry.Lowercase
  object Mode extends Enum[Mode] {
    case object Shell extends Mode

    val values = findValues
  }
  implicit val modeReuse: Reusability[Mode] = Reusability.byRef

  sealed trait ThemeStyle extends EnumEntry with EnumEntry.Lowercase
  object ThemeStyle extends Enum[ThemeStyle] {
    case object Solarized extends ThemeStyle
    case object Dark extends ThemeStyle
    case object Light extends ThemeStyle

    val values = findValues
  }
  implicit val themeStyleReuse: Reusability[ThemeStyle] = Reusability.byRef

  case class Options(
    mode: Option[Mode] = None,
    lineNumbers: Boolean = false,
    matchBrackets: Boolean = false,
    theme: Set[ThemeStyle] = Set.empty
  )
  implicit val optionsReuse: Reusability[Options] = Reusability.caseClass[Options]

  final val DefaultWidth: Width = "100%"
  final val DefaultHeight: Height = 250

  case class Props(
    text: Option[String],
    onUpdate: OnUpdate,
    width: Width,
    height: Height,
    options: Option[Options],
    attrs: Seq[TagMod]
  )
  case class State(value: Option[String])

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept('onUpdate, 'attrs)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {

    private[this] def propagateUpdate: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.value)))

    private[this] def jsOptions(props: Props): Option[js.Dynamic] = {
      props.options.map { opts =>
        js.Dynamic.literal(
          "mode" -> opts.mode.getOrElse(Mode.Shell).entryName,
          "lineNumbers" -> opts.lineNumbers,
          "matchBrackets" -> opts.matchBrackets,
          "theme" -> opts.theme.map(_.entryName).mkString(" ")
        )
      }
    }

    protected[CodeEditor] def mounted(props: Props, state: State): Unit = {
      val textArea = $.getDOMNode().asInstanceOf[html.TextArea]
      val codeMirror = CodeMirror.fromTextArea(textArea, jsOptions(props).getOrElse(js.Dynamic.literal()))

      codeMirror.setSize(props.width, props.height)
      codeMirror.on("change", (cm, event) => onChange(cm, event.asInstanceOf[ChangeEvent]))

      codeMirror.setValue(state.value.getOrElse(""))
    }

    def onChange(codeMirror: CodeMirror, change: ChangeEvent): Unit = {
      val editorValue = Option(codeMirror.getValue()).filterNot(_.isEmpty)
      $.modState(_.copy(value = editorValue), propagateUpdate).runNow()
    }

    def render(props: Props, state: State) = {
      <.textarea(^.`class` := "form-control", props.attrs)
    }

  }

  val component = ReactComponentB[Props]("CodeEditor")
    .initialState_P(props => State(props.text))
    .renderBackend[Backend]
    .componentDidMount($ => Callback { $.backend.mounted($.props, $.state) })
    .build

  private[this] def extractWidthAndHeight(attrs: Seq[TagMod]): (Width, Height, List[TagMod]) = {
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
    component(Props(value, onUpdate, width, height, None, remaining))
  }

  def apply(value: Option[String], onUpdate: OnUpdate, options: Options, attrs: TagMod*) = {
    val (width, height, remaining) = extractWidthAndHeight(attrs)
    component(Props(value, onUpdate, width, height, Some(options), remaining))
  }

}
