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

import java.time._
import java.time.format._
import java.util.concurrent.TimeUnit

import cats.instances.either._
import cats.syntax.cartesian._

import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 11/08/2016.
  */
trait TimeJson {

  implicit final val decodeInstant: Decoder[Instant] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(Instant.parse(s)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("Instant", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Instant]]
      }
    }

  implicit final val encodeInstant: Encoder[Instant] = Encoder.instance(time => Json.fromString(time.toString))

  final def decodeLocalDateTime(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalDateTime]]
      }
    }

  final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeLocalDateTimeDefault: Decoder[LocalDateTime] =
    decodeLocalDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] =
    encodeLocalDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  final def decodeZonedDateTime(formatter: DateTimeFormatter): Decoder[ZonedDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(ZonedDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("ZonedDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[ZonedDateTime]]
      }
    }

  final def encodeZonedDateTime(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeZonedDateTimeDefault: Decoder[ZonedDateTime] =
    decodeZonedDateTime(DateTimeFormatter.ISO_ZONED_DATE_TIME)
  implicit final val encodeZonedDateTimeDefault: Encoder[ZonedDateTime] =
    encodeZonedDateTime(DateTimeFormatter.ISO_ZONED_DATE_TIME)

  final def decodeOffsetDateTime(formatter: DateTimeFormatter): Decoder[OffsetDateTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(OffsetDateTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("OffsetDateTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[OffsetDateTime]]
      }
    }

  final def encodeOffsetDateTime(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeOffsetDateTimeDefault: Decoder[OffsetDateTime] =
    decodeOffsetDateTime(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  implicit final val encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] =
    encodeOffsetDateTime(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  final def decodeLocalDate(formatter: DateTimeFormatter): Decoder[LocalDate] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalDate.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalDate", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalDate]]
      }
    }

  final def encodeLocalDate(formatter: DateTimeFormatter): Encoder[LocalDate] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeLocalDateDefault: Decoder[LocalDate] =
    decodeLocalDate(DateTimeFormatter.ISO_LOCAL_DATE)
  implicit final val encodeLocalDateDefault: Encoder[LocalDate] =
    encodeLocalDate(DateTimeFormatter.ISO_LOCAL_DATE)

  final def decodeLocalTime(formatter: DateTimeFormatter): Decoder[LocalTime] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(LocalTime.parse(s, formatter)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("LocalTime", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[LocalTime]]
      }
    }

  final def encodeLocalTime(formatter: DateTimeFormatter): Encoder[LocalTime] =
    Encoder.instance(time => Json.fromString(time.format(formatter)))

  implicit final val decodeLocalTimeDefault: Decoder[LocalTime] =
    decodeLocalTime(DateTimeFormatter.ISO_LOCAL_TIME)
  implicit final val encodeLocalTimeDefault: Encoder[LocalTime] =
    encodeLocalTime(DateTimeFormatter.ISO_LOCAL_TIME)

  implicit final val decodePeriod: Decoder[Period] = Decoder.instance { c =>
    c.as[String] match {
      case Right(s) => try Right(Period.parse(s)) catch {
        case _: DateTimeParseException => Left(DecodingFailure("Period", c.history))
      }
      case l@Left(_) => l.asInstanceOf[Decoder.Result[Period]]
    }
  }

  implicit final val encodePeriod: Encoder[Period] = Encoder.instance { period =>
    Json.fromString(period.toString)
  }

  implicit def durationEncoder: Encoder[Duration] = Encoder.instance { duration =>
    Json.fromString(duration.toString)
  }
  implicit def durationDecoder: Decoder[Duration] = Decoder.instance { c =>
    c.as[String] match {
      case Right(s) => try Right(Duration.parse(s)) catch {
        case _: DateTimeParseException => Left(DecodingFailure("Duration", c.history))
      }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[Duration]]
    }
  }

  implicit final val encodeFiniteDuration: Encoder[FiniteDuration] = Encoder.instance { duration =>
    Json.obj(
      "length" -> Json.fromLong(duration.length),
      "unit"   -> Json.fromString(duration.unit.name())
    )
  }

  implicit final val decodeFiniteDuration: Decoder[FiniteDuration] = Decoder.instance { c =>
    val decodeLength = c.downField("length").as[Long]
    val decodeUnit   = c.downField("unit").as[String] match {
      case Right(s) => try Right(TimeUnit.valueOf(s)) catch {
        case _: IllegalArgumentException => Left(DecodingFailure("FiniteDuration", c.history))
      }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[TimeUnit]]
    }

    (decodeLength |@| decodeUnit).map(FiniteDuration.apply)
  }

}

object time extends TimeJson
