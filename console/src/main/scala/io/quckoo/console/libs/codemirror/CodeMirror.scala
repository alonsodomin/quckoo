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

package io.quckoo.console.libs.codemirror

import org.scalajs.dom.Element
import org.scalajs.dom.html.TextArea

import scala.scalajs.js
import scala.scalajs.js.annotation._

/**
  * Created by alonsodomin on 02/03/2017.
  */
@js.native
@JSGlobal
object CodeMirror extends js.Object {

  def apply(element: Element, options: js.Any = js.Dynamic.literal()): CodeMirror = js.native

  def defaults: js.Dynamic = js.native

  def fromTextArea(textArea: TextArea, options: js.Any = js.Dynamic.literal()): TACodeMirror = js.native

  def version: String = js.native

}

@js.native
trait CodeMirror extends js.Object {

  def getInputField(): Element = js.native

  def isReadOnly: Boolean = js.native

  def execCommand(command: String): Unit = js.native

  def hasFocus: Boolean = js.native
  def focus(): Unit = js.native

  def getOption(name: String): js.Any = js.native
  def setOption(name: String, value: js.Any): Unit = js.native

  def getLine(line: Int): String = js.native
  def lineCount: Int = js.native
  def firstLine: Int = js.native
  def lastLine: Int = js.native
  def lineSeparator: String = js.native

  def eachLine(f: LineHandle => Unit): Unit = js.native
  def eachLine(start: Int, end: Int, f: LineHandle => Unit): Unit = js.native

  def markClean(): Unit = js.native
  def changeGeneration(closeEvent: js.UndefOr[Boolean] = null): Int = js.native
  def isClean(generation: js.UndefOr[Int] = null): Boolean = js.native

  def getValue(separator: js.UndefOr[String] = null): String = js.native
  def setValue(content: String): Unit = js.native

  def setSize(width: Width, height: Height): Unit  = js.native

  def refresh(): Unit = js.native

  def on(`type`: String, handler: js.Function2[CodeMirror, js.Any, _]): Unit = js.native

}

@js.native
trait TACodeMirror extends CodeMirror {
  def getTextArea(): TextArea = js.native
  def save(): Unit = js.native
  def toTextArea(): Unit = js.native
}
