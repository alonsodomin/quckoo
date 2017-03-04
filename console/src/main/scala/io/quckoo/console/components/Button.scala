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

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Button {

  case class Props(
      onClick: Option[Callback] = None,
      disabled: Boolean = false,
      style: ContextStyle.Value = ContextStyle.default,
      addStyles: Seq[StyleA] = Seq()
  )

  case class State(enabled: Boolean = true)

  val component = ReactComponentB[Props]("Button")
    .renderPC { (_, p, children) =>
      val buttonType = if (p.onClick.isEmpty) "submit" else "button"
      <.button(
        lookAndFeel.buttonOpt(p.style),
        p.addStyles,
        ^.tpe := buttonType,
        p.onClick.map(handler => ^.onClick --> handler),
        p.disabled ?= (^.disabled := true),
        children)
    }
    .build

  def apply()                                   = component
  def apply(props: Props, children: ReactNode*) = component(props, children: _*)
}
