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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 04/07/2016.
  */
object DateTimeDisplay {

  final val DefaultFormatter = DateTimeFormatter.ofPattern(
    "MMM d, HH:mm:ss"
  )

  final case class Props(dateTime: ZonedDateTime, formatter: Option[DateTimeFormatter])

  private[this] val component = ReactComponentB[Props]("DateTimeDisplay").stateless.render_P {
    props =>
      val fmt = props.formatter.getOrElse(DefaultFormatter)
      <.span(fmt.format(props.dateTime))
  } build

  def apply(dateTime: ZonedDateTime, formatter: Option[DateTimeFormatter] = None) =
    component(Props(dateTime, formatter))

}
