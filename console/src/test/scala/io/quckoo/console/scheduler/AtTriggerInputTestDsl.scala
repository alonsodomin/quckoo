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

package io.quckoo.console.scheduler

import java.time.{LocalDate, LocalTime}

import cron4s.Cron

import io.quckoo.Trigger
import io.quckoo.console.test.ConsoleTestExports

import monocle.macros.Lenses

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.test._

object AtTriggerInputTestDsl {
  import ConsoleTestExports._

  @Lenses
  case class State(
      date: Option[LocalDate],
      time: Option[LocalTime],
      updatedTrigger: Option[Trigger.At] = None
  )

  val dsl = Dsl[Unit, AtTriggerInputObserver, State]

  def onUpdate(value: Option[Trigger.At]) = Callback {
    dsl
      .action("Update callback")
      .update(_.state)
      .updateState(_.copy(updatedTrigger = value))
  }

  def noDate =
    dsl.focus("Date").value(_.obs.dateInput.value.isEmpty)

  def noTime =
    dsl.focus("Time").value(_.obs.timeInput.value.isEmpty)

  def setDate(newDate: LocalDate): dsl.Actions =
    dsl
      .action(s"Set date: $newDate")(SimEvent.Change(newDate.toString) simulate _.obs.dateInput)
      .updateState(_.copy(date = Some(newDate)))

  def setTime(newTime: LocalTime): dsl.Actions =
    dsl
      .action(s"Set time: $newTime")(SimEvent.Change(newTime.toString) simulate _.obs.timeInput)
      .updateState(_.copy(time = Some(newTime)))
}
