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

import japgolly.scalajs.react.{ReactNode, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Alert {
  case class Props(style: ContextStyle.Value)

  val component = ReactComponentB[Props]("Alert").
    renderPC { (_, p, c) =>
      <.div(lookAndFeel.alert(p.style), ^.role := "alert", ^.padding := 5.px, c)
    } build

  def apply() = component
  def apply(style: ContextStyle.Value, children: ReactNode*) = component(Props(style), children)
}
