/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.serialization.json

import upickle.Js
import upickle.default.{Writer => JsonWriter, Reader => JsonReader, _}

import scala.language.implicitConversions

import org.threeten.bp._
import org.threeten.bp.format._

/**
  * Created by alonsodomin on 11/08/2016.
  */
trait JavaTime {

  implicit def instantW: JsonWriter[Instant] = JsonWriter[Instant] {
    x => Js.Str(DateTimeFormatter.ISO_INSTANT.format(x))
  }
  implicit def instantR: JsonReader[Instant] = JsonReader[Instant] {
    case Js.Str(fmt) => Instant.parse(fmt)
  }

  implicit def zoneIdW: JsonWriter[ZoneId] = JsonWriter[ZoneId] {
    x => Js.Str(x.getId)
  }
  implicit def zoneIdR: JsonReader[ZoneId] = JsonReader[ZoneId] {
    case Js.Str(zoneId) => ZoneId.of(zoneId)
  }

  implicit def zoneOffsetW: JsonWriter[ZoneOffset] = JsonWriter[ZoneOffset] {
    x => Js.Str(x.getId)
  }
  implicit def zoneOffsetR: JsonReader[ZoneOffset] = JsonReader[ZoneOffset] {
    case Js.Str(offsetId) => ZoneOffset.of(offsetId)
  }

  implicit def offsetDateTimeW: JsonWriter[OffsetDateTime] = JsonWriter[OffsetDateTime] {
    x => Js.Str(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(x))
  }
  implicit def offsetDateTimeR: JsonReader[OffsetDateTime] = JsonReader[OffsetDateTime] {
    case Js.Str(fmt) => OffsetDateTime.parse(fmt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  implicit def zonedDateTimeW: JsonWriter[ZonedDateTime] = JsonWriter[ZonedDateTime] {
    x => Js.Str(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(x))
  }
  implicit def zonedDateTimeR: JsonReader[ZonedDateTime] = JsonReader[ZonedDateTime] {
    case Js.Str(fmt) => ZonedDateTime.parse(fmt, DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }

  implicit def localDateTimeW: JsonWriter[LocalDateTime] = JsonWriter[LocalDateTime] {
    x => Js.Str(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(x))
  }
  implicit def localDateTimeR: JsonReader[LocalDateTime] = JsonReader[LocalDateTime] {
    case Js.Str(fmt) => LocalDateTime.parse(fmt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }

  implicit def localDateW: JsonWriter[LocalDate] = JsonWriter[LocalDate] {
    x => Js.Str(DateTimeFormatter.ISO_LOCAL_DATE.format(x))
  }
  implicit def localDateR: JsonReader[LocalDate] = JsonReader[LocalDate] {
    case Js.Str(fmt) => LocalDate.parse(fmt, DateTimeFormatter.ISO_DATE)
  }

  implicit def localTimeW: JsonWriter[LocalTime] = JsonWriter[LocalTime] {
    x => Js.Str(DateTimeFormatter.ISO_LOCAL_TIME.format(x))
  }
  implicit def localTimeR: JsonReader[LocalTime] = JsonReader[LocalTime] {
    case Js.Str(fmt) => LocalTime.parse(fmt, DateTimeFormatter.ISO_LOCAL_TIME)
  }

  implicit def durationW: JsonWriter[Duration] = JsonWriter[Duration] {
    x => Js.Str(x.toString())
  }
  implicit def durationR: JsonReader[Duration] = JsonReader[Duration] {
    case Js.Str(fmt) => Duration.parse(fmt)
  }

  implicit def periodW: JsonWriter[Period] = JsonWriter[Period] {
    x => Js.Str(x.toString())
  }
  implicit def periodR: JsonReader[Period] = JsonReader[Period] {
    case Js.Str(fmt) => Period.parse(fmt)
  }

}
