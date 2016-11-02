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

package io.quckoo.validation

import upickle.Js
import upickle.default.{Reader => UReader, Writer => UWriter, _}

import io.quckoo.serialization.json._

import enumeratum.{Enum => EEnum}

import scalaz._
import Scalaz._

sealed trait Violation

object Violation {
  case class And(left: Violation, right: Violation) extends Violation
  case class Or(left: Violation, right: Violation)  extends Violation

  case class EqualTo(expected: String, actual: String)       extends Violation
  case class GreaterThan(expected: String, actual: String)   extends Violation
  case class LessThan(expected: String, actual: String)      extends Violation
  case class MemberOf(expected: Set[String], actual: String) extends Violation

  case object Empty     extends Violation
  case object Undefined extends Violation

  implicit def display: Show[Violation] = Show.shows {
    case And(left, right) => s"${left.shows} and ${right.shows}"
    case Or(left, right)  => s"${left.shows} or ${right.shows}"

    case EqualTo(expected, actual)     => s"$actual != $expected"
    case GreaterThan(expected, actual) => s"$actual < $expected"
    case LessThan(expected, actual)    => s"$actual > $expected"
    case MemberOf(expected, actual)    => s"$actual not in $expected"

    case Empty     => "non empty"
    case Undefined => "not defined"

    case p: PathViolation => p.shows
  }

  object conjunction {
    implicit val violationConjSemigroup: Semigroup[Violation] = new Semigroup[Violation] {
      def append(left: Violation, right: => Violation): Violation = And(left, right)
    }
  }

  object disjunction {
    implicit val violationDisjSemigroup: Semigroup[Violation] = new Semigroup[Violation] {
      def append(left: Violation, right: => Violation): Violation = Or(left, right)
    }
  }

  implicit class ViolationSyntax(val self: Violation) extends AnyVal {
    def and(other: Violation): Violation = And(self, other)
    def or(other: Violation): Violation  = Or(self, other)
  }

}

case class PathViolation(path: Path, violation: Violation) extends Violation

object PathViolation {
  import Violation._

  def at(path: Path, violation: Violation): Violation = violation match {
    case PathViolation(otherPath, otherViolation) =>
      PathViolation(path ++ otherPath, otherViolation)

    case And(left, right) =>
      And(at(path, left), at(path, right))
    case Or(left, right) =>
      Or(at(path, left), at(path, right))

    case _ => PathViolation(path, violation)
  }

  implicit val pathViolationShow: Show[PathViolation] = Show.shows { value =>
    val violationsDesc = value.violation.shows
    s"expected $violationsDesc at ${value.path.shows}"
  }

  implicit def jsonWriter: UWriter[PathViolation] = UWriter[PathViolation] { pv =>
    Js.Obj(
      "path"      -> Path.pathJsonWriter.write(pv.path),
      "violation" -> implicitly[UWriter[Violation]].write(pv.violation),
      "$type"     -> Js.Str(classOf[PathViolation].getName)
    )
  }

  implicit def jsonReader: UReader[PathViolation] = UReader[PathViolation] {
    val pathReader       = Kleisli(Path.pathJsonReader.read.lift)
    val violationsReader = Kleisli(implicitly[UReader[Violation]].read.lift)

    val prod = Kleisli[Option, (Js.Value, Js.Value), PathViolation] {
      case (path, violation) =>
        (pathReader.run(path) |@| violationsReader.run(violation))((p, v) => PathViolation(p, v))
    }

    val extractFieldMap: PartialFunction[Js.Value, Map[String, Js.Value]] = {
      case obj: Js.Obj => obj.value.toMap
    }
    val extractJsValues = Kleisli(extractFieldMap.lift).flatMapK { fields =>
      (fields.get("path") |@| fields.get("violation"))(_ -> _)
    }

    Function.unlift(prod.compose(extractJsValues).run)
  }

}
