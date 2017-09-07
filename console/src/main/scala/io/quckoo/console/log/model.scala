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

package io.quckoo.console.log

import java.time.{Clock, ZonedDateTime}

import cats.Show

import enumeratum._
import enumeratum.values._

sealed abstract class LogLevel(val value: Short) extends ShortEnumEntry with EnumEntry.Uppercase
object LogLevel extends ShortEnum[LogLevel] {
  case object Info    extends LogLevel(0)
  case object Warning extends LogLevel(1)
  case object Error   extends LogLevel(2)

  val values = findValues
}

case class LogRecord(level: LogLevel, when: ZonedDateTime, message: String)

object LogRecord {

  def info(message: String)(implicit clock: Clock): LogRecord =
    LogRecord(LogLevel.Info, ZonedDateTime.now(clock), message)

  def info(when: ZonedDateTime, message: String): LogRecord =
    LogRecord(LogLevel.Info, when, message)

  def warning(message: String)(implicit clock: Clock): LogRecord =
    LogRecord(LogLevel.Warning, ZonedDateTime.now(clock), message)

  def error(when: ZonedDateTime, message: String): LogRecord =
    LogRecord(LogLevel.Error, when, message)

  def error(message: String)(implicit clock: Clock): LogRecord =
    LogRecord(LogLevel.Error, ZonedDateTime.now(clock), message)

  implicit val logRecordShow: Show[LogRecord] = Show.show { record =>
    s"${record.when} - [${record.level.entryName}] - ${record.message}"
  }

}
