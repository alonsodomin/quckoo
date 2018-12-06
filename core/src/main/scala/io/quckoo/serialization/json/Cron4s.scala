/*
 * Copyright 2015 A. Alonso Dominguez
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

import cats.implicits._

import cron4s._
import cron4s.expr._

import io.circe.{Encoder, Decoder, DecodingFailure}

/**
  * Created by alonsodomin on 03/09/2016.
  */
trait Cron4s {

  implicit val cronExprEncoder: Encoder[CronExpr] =
    Encoder[String].contramap(_.toString)

  implicit val cronExprDecoder: Decoder[CronExpr] = Decoder.instance { c =>
    c.as[String] match {
      case Right(expr) => Cron(expr).leftMap(_ => DecodingFailure("Cron", c.history))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[CronExpr]]
    }
  }

}

object cron extends Cron4s
