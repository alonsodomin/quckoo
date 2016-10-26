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

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.{DateTimeFormatter, FormatStyle}
import org.threeten.bp.temporal.Temporal

/**
  * Created by alonsodomin on 04/07/2016.
  */
object DateTimeDisplay {

  private[this] final val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)

  final case class Props(dateTime: Option[ZonedDateTime], useLocal: Boolean)

  private[this] val component = ReactComponentB[Props]("DateTimeDisplay").stateless.render_P {
    props =>
      /*val dt: Option[Temporal] = {
        if (props.useLocal) props.dateTime.map(_.toLocalDateTime)
        else props.dateTime
      }*/
      <.span(props.dateTime.map(formatter.format))
  } build

  def apply(dateTime: Option[ZonedDateTime], useLocal: Boolean = true) =
    component(Props(dateTime, useLocal))

}