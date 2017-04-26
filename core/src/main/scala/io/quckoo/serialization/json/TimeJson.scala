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
      c.as[Long] match {
        case Right(s) => try Right(Instant.ofEpochMilli(s)) catch {
          case _: DateTimeParseException => Left(DecodingFailure("Instant", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Instant]]
      }
    }

  implicit final val encodeInstant: Encoder[Instant] = Encoder.instance(time => Json.fromLong(time.toEpochMilli))

  implicit final val decodeLocalDateTimeDefault: Decoder[LocalDateTime] =
    Decoder[Instant].map(time => LocalDateTime.ofInstant(time, ZoneOffset.UTC))
  implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] =
    Encoder[Instant].contramap(_.toInstant(ZoneOffset.UTC))

  implicit final val decodeZonedDateTimeDefault: Decoder[ZonedDateTime] =
    Decoder[Instant].map(time => ZonedDateTime.ofInstant(time, ZoneOffset.UTC))
  implicit final val encodeZonedDateTimeDefault: Encoder[ZonedDateTime] =
    Encoder[Instant].contramap(_.withZoneSameInstant(ZoneOffset.UTC).toInstant)

  implicit final val decodeOffsetDateTimeDefault: Decoder[OffsetDateTime] =
    Decoder[Instant].map(time => OffsetDateTime.ofInstant(time, ZoneOffset.UTC))
  implicit final val encodeOffsetDateTimeDefault: Encoder[OffsetDateTime] =
    Encoder[Instant].contramap(_.atZoneSameInstant(ZoneOffset.UTC).toInstant)

  implicit final val decodeLocalDateDefault: Decoder[LocalDate] =
    Decoder[Long].map(LocalDate.ofEpochDay)
  implicit final val encodeLocalDateDefault: Encoder[LocalDate] =
    Encoder[Long].contramap(_.toEpochDay)

  implicit final val decodeLocalTimeDefault: Decoder[LocalTime] =
    Decoder[Long].map(LocalTime.ofNanoOfDay)
  implicit final val encodeLocalTimeDefault: Encoder[LocalTime] =
    Encoder[Long].contramap(_.toNanoOfDay)

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
