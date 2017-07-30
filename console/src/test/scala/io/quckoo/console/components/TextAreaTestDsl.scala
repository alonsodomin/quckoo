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

import io.quckoo.console.test.ConsoleTestExports

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test._

/**
  * Created by alonsodomin on 25/02/2017.
  */
object TextAreaTestDsl {
  import ConsoleTestExports._

  final case class TextAreaState(text: Option[String] = None)

  val dsl = Dsl[Unit, TextAreaObserver, TextAreaState]

  def blankInput = dsl.test("Blank input")(_.obs.textArea.value.isEmpty)

  def emptyText = dsl.focus("Text").value(_.obs.textArea.value.isEmpty)

  def setText(text: String): dsl.Actions =
    dsl
      .action(s"Set text: $text")(SimEvent.Change(text) simulate _.obs.textArea)
      .updateState(_.copy(Some(text)))

  def onUpdate(value: Option[String]): Callback = Callback {
    dsl
      .action("Update callback")
      .update(_.state)
      .updateState(_.copy(text = value))
  }

}
