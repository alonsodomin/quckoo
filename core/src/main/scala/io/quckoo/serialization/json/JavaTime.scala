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
import upickle.default.{Writer => UWriter, Reader => UReader, _}

import scala.language.implicitConversions

import org.threeten.bp._
import org.threeten.bp.format._

/**
  * Created by alonsodomin on 11/08/2016.
  */
trait JavaTime {

  implicit def instantW: UWriter[Instant] = UWriter[Instant] {
    x => Js.Str(DateTimeFormatter.ISO_INSTANT.format(x))
  }
  implicit def instantR: UReader[Instant] = UReader[Instant] {
    case Js.Str(fmt) => Instant.parse(fmt)
  }

  implicit def zoneIdW: UWriter[ZoneId] = UWriter[ZoneId] {
    x => Js.Str(x.getId)
  }
  implicit def zoneIdR: UReader[ZoneId] = UReader[ZoneId] {
    case Js.Str(zoneId) => ZoneId.of(zoneId)
  }

  implicit def zoneOffsetW: UWriter[ZoneOffset] = UWriter[ZoneOffset] {
    x => Js.Str(x.getId)
  }
  implicit def zoneOffsetR: UReader[ZoneOffset] = UReader[ZoneOffset] {
    case Js.Str(offsetId) => ZoneOffset.of(offsetId)
  }

  implicit def offsetDateTimeW: UWriter[OffsetDateTime] = UWriter[OffsetDateTime] {
    x => Js.Str(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(x))
  }
  implicit def offsetDateTimeR: UReader[OffsetDateTime] = UReader[OffsetDateTime] {
    case Js.Str(fmt) => OffsetDateTime.parse(fmt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  implicit def zonedDateTimeW: UWriter[ZonedDateTime] = UWriter[ZonedDateTime] {
    x => Js.Arr(writeJs(x.toInstant), writeJs(x.getZone))
  }
  implicit def zonedDateTimeR: UReader[ZonedDateTime] = UReader[ZonedDateTime] {
    case Js.Arr(inst, zid) =>
      val instant = readJs[Instant](inst)
      val zoneId  = readJs[ZoneId](zid)
      ZonedDateTime.ofInstant(instant, zoneId)
  }

  implicit def localDateTimeW: UWriter[LocalDateTime] = UWriter[LocalDateTime] {
    x => Js.Str(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(x))
  }
  implicit def localDateTimeR: UReader[LocalDateTime] = UReader[LocalDateTime] {
    case Js.Str(fmt) => LocalDateTime.parse(fmt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }

  implicit def localDateW: UWriter[LocalDate] = UWriter[LocalDate] {
    x => Js.Str(DateTimeFormatter.ISO_LOCAL_DATE.format(x))
  }
  implicit def localDateR: UReader[LocalDate] = UReader[LocalDate] {
    case Js.Str(fmt) => LocalDate.parse(fmt, DateTimeFormatter.ISO_DATE)
  }

  implicit def localTimeW: UWriter[LocalTime] = UWriter[LocalTime] {
    x => Js.Str(DateTimeFormatter.ISO_LOCAL_TIME.format(x))
  }
  implicit def localTimeR: UReader[LocalTime] = UReader[LocalTime] {
    case Js.Str(fmt) => LocalTime.parse(fmt, DateTimeFormatter.ISO_LOCAL_TIME)
  }

  implicit def durationW: UWriter[Duration] = UWriter[Duration] {
    x => Js.Str(x.toString())
  }
  implicit def durationR: UReader[Duration] = UReader[Duration] {
    case Js.Str(fmt) => Duration.parse(fmt)
  }

  implicit def periodW: UWriter[Period] = UWriter[Period] {
    x => Js.Str(x.toString())
  }
  implicit def periodR: UReader[Period] = UReader[Period] {
    case Js.Str(fmt) => Period.parse(fmt)
  }

}
