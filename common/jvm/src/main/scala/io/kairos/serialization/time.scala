package io.kairos.serialization

import java.time.{ZonedDateTime, Instant, ZoneId}

import io.kairos.time.{JDK8DateTime, DateTime}

/**
  * Created by alonsodomin on 14/03/2016.
  */
object time {
  import upickle.Js
  import upickle.default._

  implicit val writer: Writer[DateTime] = Writer[DateTime] {
    case dateTime =>
      writeJs(dateTime.toUTC.toEpochMillis)
  }
  implicit val reader: Reader[DateTime] = Reader[DateTime] {
    case Js.Num(millis) =>
      val instant = Instant.ofEpochMilli(millis.toLong)
      val zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")).
        withZoneSameInstant(ZoneId.systemDefault())
      new JDK8DateTime(zonedDateTime = zdt)
  }
}
