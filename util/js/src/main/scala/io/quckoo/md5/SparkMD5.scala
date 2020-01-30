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

package io.quckoo.md5

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSImport("spark-md5", JSImport.Default)
object SparkMD5 extends js.Object {

  def hash(str: String, raw: Boolean = false): String                            = js.native
  def hashBinary(chunk: js.typedarray.ArrayBuffer, raw: Boolean = false): String = js.native

  @js.native
  class ArrayBuffer() extends js.Object {
    def append(chunk: js.typedarray.ArrayBuffer): Unit = js.native
    def end(raw: Boolean = false): String              = js.native
  }

}
