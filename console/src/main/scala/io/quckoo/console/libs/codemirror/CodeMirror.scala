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

import org.scalajs.dom.html.TextArea

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Created by alonsodomin on 02/03/2017.
  */
@js.native
object CodeMirror extends js.Object {

  def fromTextArea(textArea: TextArea, options: js.Any = js.Dynamic.literal()): CodeMirror = js.native

}

@js.native
trait CodeMirror extends js.Object {

  def getValue(separator: js.UndefOr[String] = null): String = js.native
  def setValue(content: String): this.type = js.native

  def setSize(width: Width, height: Height): this.type  = js.native

  def on(`type`: String, handler: js.Function2[CodeMirror, js.Any, _]): this.type = js.native

}
