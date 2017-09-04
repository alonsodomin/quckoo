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

package io.quckoo.console.layout

import diode.react.ModelProxy

import io.quckoo.console.core.{ConsoleCircuit, ConsoleScope}
import io.quckoo.console.log.{LogDisplay, LogRecord}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import monix.reactive.Observable

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 14/05/2017.
  */
object Footer {
  import CssSettings._
  import ConsoleCircuit.Implicits.consoleClock

  object Style extends StyleSheet.Inline {
    import dsl._

    val footer = style(
      position.absolute,
      bottom.`0`,
      width(100 %%),
      height(60 px),
      backgroundColor(c"#f5f5f5"),
      padding(20 px, 25 px)
    )
  }

  case class Props(proxy: ModelProxy[ConsoleScope], logStream: Observable[LogRecord])

  private[this] val component = ScalaComponent
    .builder[Props]("Footer")
    .stateless
    .render_P { props =>
      <.div(
        props
          .proxy()
          .passport
          .flatMap(_.subject)
          .map { _ =>
            <.footer(Style.footer, LogDisplay(props.logStream))
          }
          .whenDefined
      )
    }
    .build

  def apply(proxy: ModelProxy[ConsoleScope], logStream: Observable[LogRecord]) =
    component(Props(proxy, logStream))

}
