package io.chronos.cron

import java.time.ZonedDateTime
import java.time.temporal.{ChronoField, TemporalAccessor, TemporalField}

/**
 * Created by aalonsodominguez on 02/08/15.
 */
sealed trait FieldExpr {
  val field: TemporalField
  val matcher: FieldMatcher

  def nextMatch(from: TemporalAccessor)(implicit now: ZonedDateTime): Int = {
    import FieldMatcher._

    val fromVal = field.getFrom(from)
    val nowVal  = field.getFrom(now)
    matcher match {
      case Any      => nowVal.toInt
      case e: Every => (fromVal + e.frequency).toInt
      case e: Exact => e.value
    }
  }
}

object FieldExpr {
  import FieldMatcher._

  private abstract class FieldExprFactory[T <: FieldExpr] {

    final def apply(expr: String): T = {
      if ("*" == expr) make(Any)
      else mappedVal(expr) match {
        case Some(v) => make(Exact(v))
        case None    =>
          if (expr.startsWith("*/")) make(Every(expr.substring(2).toInt))
          else make(Exact(expr.toInt))
      }
    }

    protected def make(matcher: FieldMatcher): T
    protected def mappedVal(v: String): Option[Int] = None

  }

  case class MinuteExpr(matcher: FieldMatcher) extends FieldExpr {
    val field = ChronoField.MINUTE_OF_HOUR
  }
  object MinuteExpr extends FieldExprFactory[MinuteExpr] {
    override protected def make(matcher: FieldMatcher): MinuteExpr = MinuteExpr(matcher)
  }

  case class HourExpr(matcher: FieldMatcher) extends FieldExpr {
    val field = ChronoField.HOUR_OF_DAY
  }
  object HourExpr extends FieldExprFactory[HourExpr] {
    override protected def make(matcher: FieldMatcher): HourExpr = HourExpr(matcher)
  }

  case class DayOfMonthExpr(matcher: FieldMatcher) extends FieldExpr {
    val field = ChronoField.DAY_OF_MONTH
  }
  object DayOfMonthExpr extends FieldExprFactory[DayOfMonthExpr] {
    override protected def make(matcher: FieldMatcher): DayOfMonthExpr = DayOfMonthExpr(matcher)
  }

  case class MonthExpr(matcher: FieldMatcher) extends FieldExpr  {
    val field = ChronoField.MONTH_OF_YEAR
  }
  object MonthExpr extends FieldExprFactory[MonthExpr] {
    private val Names = Map(
      "jan" -> 1, "feb" -> 2, "mar" -> 3, "apr" -> 4, "may" -> 5, "jun" -> 6, "jul" -> 7, "ago" -> 8,
      "sep" -> 9, "oct" -> 10, "nov" -> 11, "dec" -> 12
    )

    override protected def make(matcher: FieldMatcher): MonthExpr = MonthExpr(matcher)

    override protected def mappedVal(v: String): Option[Int] = Names.get(v)
  }

  case class DayOfWeekExpr(matcher: FieldMatcher) extends FieldExpr {
    val field = ChronoField.DAY_OF_WEEK
  }
  object DayOfWeekExpr extends FieldExprFactory[DayOfWeekExpr] {
    private val Names = Map(
      "sun" -> 0, "mon" -> 1, "tue" -> 2, "wed" -> 3, "thu" -> 4, "fri" -> 5, "sat" -> 6
    )

    override protected def make(matcher: FieldMatcher): DayOfWeekExpr = DayOfWeekExpr(matcher)

    override protected def mappedVal(v: String): Option[Int] = Names.get(v)
  }

  case class YearExpr(matcher: FieldMatcher) extends FieldExpr {
    val field = ChronoField.YEAR
  }
  object YearExpr extends FieldExprFactory[YearExpr] {
    override protected def make(matcher: FieldMatcher): YearExpr = YearExpr(matcher)
  }

}
