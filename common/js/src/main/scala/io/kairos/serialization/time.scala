package io.kairos.serialization

import io.kairos.time.{DateTime, MomentJSDateTime}
import org.widok.moment.Moment

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
      new MomentJSDateTime(Moment.utc(millis))
  }

}
