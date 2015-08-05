package io.chronos.scheduler.internal

import java.time.{Clock, Duration, Instant, ZoneId}

/**
 * Created by aalonsodominguez on 05/08/15.
 */
object ClockParser {

  val ClockRegEx = """(.+?)\[(.+)]""".r

  def apply(clockDef: String): Clock = {
    val ClockRegEx(clockName, details) = clockDef
    clockName match {
      case "SystemClock" =>
        Clock.system(ZoneId.of(details))
      case "FixedClock" =>
        val (instant, zoneId) = details.splitAt(details.indexOf(','))
        Clock.fixed(Instant.parse(instant), ZoneId.of(zoneId))
      case "OffsetClock" =>
        val (baseClock, offset) = details.splitAt(details.indexOf(','))
        Clock.offset(ClockParser(baseClock), Duration.parse(offset))
      case "TickClock" =>
        val (baseClock, tick) = details.splitAt(details.indexOf(','))
        Clock.tick(ClockParser(baseClock), Duration.parse(tick))
    }
  }

}
