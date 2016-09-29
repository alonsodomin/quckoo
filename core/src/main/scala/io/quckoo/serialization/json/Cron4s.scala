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

  implicit def cronExprW: UWriter[CronExpr] = UWriter[CronExpr] {
    expr => Js.Str(expr.toString)
  }

  implicit def cronExprR: UReader[CronExpr] = UReader[CronExpr] {
    def extractExpr: PartialFunction[Js.Value, String] = {
      case Js.Str(expr) => expr
    }
    def parseExpr(input: String): Option[CronExpr] = Cron(input).disjunction.toOption

    Function.unlift(extractExpr.lift.andThen(_.flatMap(parseExpr)))
  }

}
