package io.quckoo.serialization.json

import io.quckoo.time.{DateTime, MomentJSDateTime}
import org.widok.moment.Moment

import upickle.Js
import upickle.default._

/**
  * Created by alonsodomin on 19/03/2016.
  */
trait JSTimeJson {

  implicit val writer: Writer[DateTime] = Writer[DateTime] {
    case dateTime =>
      Js.Num(dateTime.toUTC.toEpochMillis)
  }

  implicit val reader: Reader[DateTime] = Reader[DateTime] {
    case Js.Num(millis) =>
      new MomentJSDateTime(Moment.utc(millis))
  }

}
