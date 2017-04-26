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

package io.quckoo.console.layout

import io.quckoo.console.components._

import CssSettings._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  val bootstrap = lookAndFeel

  val pageToolbar = style(
    marginTop(15 px),
    marginBottom(10 px)
  )

}
