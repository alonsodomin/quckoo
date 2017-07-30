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

import java.util.concurrent.TimeUnit

import io.quckoo.console.test.ConsoleTestExports._

import org.scalajs.dom.html

/**
  * Created by alonsodomin on 26/02/2017.
  */
class FiniteDurationInputObserver(id: String, $ : HtmlDomZipper) {

  val lengthInput = $(s"input#${id}_length").domAs[html.Input]

  val unitSelect = $(s"select#${id}_unit").domAs[html.Select]

  val unitOpts =
    $.collect1n("option[value!='']").mapZippers(_.domAs[html.Option])
  def unitOpt(unit: TimeUnit) = unitOpts.find(_.value == unit.name()).get

  val units = unitOpts.map(opt => TimeUnit.valueOf(opt.value))

  val selectedUnitOpt = unitOpts.find(_.value == unitSelect.value)

  val validationBlock =
    $.collect01(".help-block").mapZippers(_.domAs[html.Span])

}
