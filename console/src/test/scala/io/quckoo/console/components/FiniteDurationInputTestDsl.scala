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

import io.quckoo.console.test.ConsoleTestExports

import japgolly.scalajs.react.test._

import monocle.macros.Lenses

import org.scalajs.dom.html

import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by alonsodomin on 26/02/2017.
  */
object FiniteDurationInputTestDsl {
  import ConsoleTestExports._

  @Lenses
  case class State(length: Option[Long] = None, unit: Option[TimeUnit] = None)

  val dsl = Dsl[Unit, FiniteDurationInputObserver, State]

  def currentLength = {
    def lengthValue(input: html.Input): Option[Long] =
      if (input.value.isEmpty) None
      else Try(input.value.toLong).toOption

    dsl.focus("Length").value(os => lengthValue(os.obs.lengthInput))
  }

  def selectedUnit =
    dsl.focus("Selected Unit").value(_.obs.selectedUnitOpt.map(opt => TimeUnit.valueOf(opt.value)))

  def validUnitOffer = dsl.test("Offers valid units")(_.obs.units.containsSlice(
    Vector(MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)
  ))

  def setLength(length: Long) =
    dsl.action(s"Set length: $length")(SimEvent.Change(length.toString) simulate _.obs.lengthInput)
      .updateState(State.length.set(Some(length)))

  def clearLength() =
    dsl.action("Clear length")(SimEvent.Change("") simulate _.obs.lengthInput)
      .updateState(State.length.set(None))

  def chooseUnit(unit: TimeUnit) =
    dsl.action(s"Choose unit: $unit")(SimEvent.Change(unit.name()) simulate _.obs.unitSelect)
      .updateState(State.unit.set(Some(unit)))

  def validationMsg =
    dsl.focus("Validation Message").value(_.obs.validationBlock)

}
