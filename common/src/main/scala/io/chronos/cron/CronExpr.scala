package io.chronos.cron

import java.time.{Clock, ZonedDateTime}

import io.chronos.cron.FieldExpr._

/**
 * Created by aalonsodominguez on 02/08/15.
 */
class CronExpr private (minute: MinuteExpr, hour: HourExpr, dayOfMonth: DayOfMonthExpr,
                        month: MonthExpr, dayOfWeek: DayOfWeekExpr, year: Option[YearExpr]) {

  def next(from: ZonedDateTime)(implicit clock: Clock): ZonedDateTime = {
    implicit val now = ZonedDateTime.now(clock)
    val minuteVal     = minute.nextMatch(from)
    val hourVal       = hour.nextMatch(from)
    val dayOfMonthVal = dayOfMonth.nextMatch(from)
    val monthVal      = month.nextMatch(from)
    val dayOfWeekVal  = dayOfWeek.nextMatch(from)
    val yearVal       = year.map(_.nextMatch(from)).getOrElse(now.getYear)
    ZonedDateTime.of(yearVal, monthVal, dayOfMonthVal, hourVal, minuteVal, now.getSecond, now.getNano, clock.getZone)
  }

}

object CronExpr {
  
  private object Pattern {

    val Minute     = "(\\*|[0-5]?[0-9]|\\*\\/[0-9]+)"
    val Hour       = "(\\*|1?[0-9]|2[0-3]|\\*\\/[0-9]+)"
    val DayOfMonth = "(\\*|[1-2]?[0-9]|3[0-1]|\\*\\/[0-9]+)"
    val Month      = "(\\*|[0-9]|1[0-2]|\\*\\/[0-9]+|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)"
    val DayOfWeek  = "(\\*\\/[0-9]+|\\*|[0-6]|sun|mon|tue|wed|thu|fri|sat)"
    val Year       = "(\\*\\/[0-9]+|\\*|[0-9]+)"

    val Cron       = s"$Minute\\s+$Hour\\s+$DayOfMonth\\s+$Month\\s+$DayOfWeek(\\s+$Year)?".r(
      "minute", "hour", "dayOfMonth", "month", "dayOfWeek", "year"
    )

  }

  def apply(expr: String): CronExpr = {
    val Pattern.Cron(minute, hour, dayOfMonth, month, dayOfWeek, year) = expr
    new CronExpr(MinuteExpr(minute), HourExpr(hour), DayOfMonthExpr(dayOfMonth), MonthExpr(month), DayOfWeekExpr(dayOfWeek), None)
  }

  def isValid(expr: String): Boolean = expr.matches(Pattern.Cron.toString())

}