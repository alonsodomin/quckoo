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

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 05/07/2016.
  */
object NotificationContainer {

  val dataNotify = VdomAttr("data-notify")

  private[this] val component = ScalaComponent
    .builder[Unit]("NotificationContainer")
    .stateless
    .render(
      $ =>
        <.div(
          dataNotify := "container",
          ^.`class` := "col-xs-11 col-sm-3 alert alert-{0}",
          ^.role := "alert",
          <.button(
            ^.`type` := "button",
            ^.aria.hidden := true,
            ^.`class` := "close",
            dataNotify := "dismiss",
            "x"
          ),
          <.span(dataNotify := "icon"),
          <.span(dataNotify := "title", "{1}"),
          <.span(dataNotify := "message", "{2}"),
          <.div(
            ^.`class` := "progress",
            dataNotify := "progressbar",
            <.div(
              ^.`class` := "progress-bar progress-bar-{0}",
              ^.role := "progressbar",
              ^.aria.valueNow := "0",
              ^.aria.valueMin := "0",
              ^.aria.valueMax := "100",
              ^.width := "0%"
            )
          ),
          <.a(^.href := "{3}", ^.target := "{4}", dataNotify := "url")
      )
  ) build

  def apply() = component()

}
