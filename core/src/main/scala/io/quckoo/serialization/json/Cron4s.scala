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

import cron4s._
import cron4s.expr._

import upickle.Js
import upickle.default.{Reader => UReader, Writer => UWriter}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 03/09/2016.
  */
trait Cron4s {

  implicit def cronExprW: UWriter[CronExpr] = UWriter[CronExpr] { expr =>
    Js.Str(expr.toString)
  }

  implicit def cronExprR: UReader[CronExpr] = UReader[CronExpr] {
    def extractExpr: PartialFunction[Js.Value, String] = {
      case Js.Str(expr) => expr
    }
    def parseExpr(input: String): Option[CronExpr] = Cron(input).disjunction.toOption

    Function.unlift(extractExpr.lift.andThen(_.flatMap(parseExpr)))
  }

}
