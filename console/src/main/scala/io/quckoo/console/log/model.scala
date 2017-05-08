package io.quckoo.console.log

import java.time.{Clock, ZonedDateTime}

import diode.ActionType

import enumeratum._
import enumeratum.values._

sealed abstract class LogLevel(val value: Short) extends ShortEnumEntry with EnumEntry.Uppercase
object LogLevel extends ShortEnum[LogLevel] {
  case object Info extends LogLevel(0)
  case object Warning extends LogLevel(1)
  case object Error extends LogLevel(2)

  val values = findValues
}

case class LogRecord(level: LogLevel, when: ZonedDateTime, message: String)

object LogRecord {

  def info(message: String)(implicit clock: Clock): LogRecord =
    LogRecord(LogLevel.Info, ZonedDateTime.now(clock), message)

  def warning(message: String)(implicit clock: Clock): LogRecord =
    LogRecord(LogLevel.Warning, ZonedDateTime.now(clock), message)

  def error(message: String)(implicit clock: Clock): LogRecord =
    LogRecord(LogLevel.Error, ZonedDateTime.now(clock), message)

  implicit object actionType extends ActionType[LogRecord]
}