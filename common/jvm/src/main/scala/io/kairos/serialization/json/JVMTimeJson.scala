package io.kairos.serialization.json

import java.time.{Instant, ZoneId, ZonedDateTime}

import io.kairos.time.{DateTime, JDK8DateTime}

import upickle.Js
import upickle.default._

/**
  * Created by alonsodomin on 19/03/2016.
  */
trait JVMTimeJson {

  implicit val writer: Writer[DateTime] = Writer[DateTime] {
    case dateTime =>
      Js.Num(dateTime.toUTC.toEpochMillis)
  }

  implicit val reader: Reader[DateTime] = Reader[DateTime] {
    case Js.Num(millis) =>
      val instant = Instant.ofEpochMilli(millis.toLong)
      val zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")).
        withZoneSameInstant(ZoneId.systemDefault())
      new JDK8DateTime(zonedDateTime = zdt)
  }

}
