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

import cats._
import cats.implicits._

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto._

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

  case class Reject(value: String) extends Violation

  val violationConjEquality: Eq[And] = Eq.instance { (a, b) =>
    (a.left === b.left && a.right === b.right) || (a.left === b.right && a.right === b.left)
  }
  val violationDisjEquality: Eq[Or] = Eq.instance { (a, b) =>
    (a.left === b.left && a.right === b.right) || (a.left === b.right && a.right === b.left)
  }

  implicit val violationEq: Eq[Violation] = Eq.instance {
    case (left @ And(And(aLeft, bLeft), cLeft), right @ And(aRight, And(bRight, cRight))) =>
      violationConjEquality.eqv(left, And(And(aRight, bRight), cRight)) &&
      violationConjEquality.eqv(And(aLeft, And(bLeft, cLeft)), right)

    case (left @ Or(Or(aLeft, bLeft), cLeft), right @ Or(aRight, Or(bRight, cRight))) =>
      violationDisjEquality.eqv(left, Or(Or(aRight, bRight), cRight)) ||
      violationDisjEquality.eqv(Or(aLeft, Or(bLeft, cLeft)), right)

    case (left @ And(_, _), right @ And(_, _)) =>
      violationConjEquality.eqv(left, right)
    case (left @ Or(_, _), right @ Or(_, _)) =>
      violationDisjEquality.eqv(left, right)

    case (left, right) => left == right
  }

  implicit def violationShow: Show[Violation] = Show.show {
    case And(left, right) => s"${left.show} and ${right.show}"
    case Or(left, right)  => s"${left.show} or ${right.show}"

    case EqualTo(expected, actual)     => s"$actual != $expected"
    case GreaterThan(expected, actual) => s"$actual < $expected"
    case LessThan(expected, actual)    => s"$actual > $expected"
    case MemberOf(expected, actual)    => s"$actual not in $expected"

    case Empty     => "non empty"
    case Undefined => "not defined"

    case Reject(value) => s"not $value"

    case p: PathViolation => p.show
  }

  implicit def violationEncoder: Encoder[Violation] = Encoder.instance {
    case p: PathViolation => PathViolation.jsonEncoder.apply(p)
    case v                => deriveEncoder[Violation].apply(v)
  }

  implicit def violationDecoder: Decoder[Violation] = Decoder.instance {
    cursor => PathViolation.jsonDecoder.or(deriveDecoder[Violation]).apply(cursor)
  }

  implicit class ViolationSyntax(val self: Violation) extends AnyVal {
    def and(other: Violation): Violation = And(self, other)
    def or(other: Violation): Violation  = self match {
      case Reject(_) => other
      case _ => other match {
        case Reject(_) => self
        case _ => Or(self, other)
      }
    }
  }

  object conjunction {
    implicit val violationConjSemigroup: Semigroup[Violation] =
      (left: Violation, right: Violation) => left and right
  }

  object disjunction {
    implicit val violationDisjSemigroup: Semigroup[Violation] =
      (left: Violation, right: Violation) => left or right
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

  implicit val pathViolationShow: Show[PathViolation] = Show.show { value =>
    val violationsDesc = value.violation.show
    s"expected $violationsDesc at ${value.path.show}"
  }

  implicit def jsonEncoder: Encoder[PathViolation] = Encoder.instance { pv =>
    Json.obj(
      "path"      -> Path.pathJsonEncoder.apply(pv.path),
      "violation" -> Encoder[Violation].apply(pv.violation)
    )
  }

  implicit def jsonDecoder: Decoder[PathViolation] = Decoder.instance { cursor =>
    (cursor.downField("path").as[Path] |@| cursor.downField("violation").as[Violation]).map { (path, violation) =>
      PathViolation(path, violation)
    }
  }

}
