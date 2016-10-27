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

import scalaz._
import Scalaz._

sealed trait Violation

object Violation {
  case class GreaterThan(expected: String, actual: String) extends Violation
  case class LessThan(expected: String, actual: String) extends Violation
  case class MemberOf(expected: Set[String], actual: String) extends Violation

  case object Empty extends Violation
  case object Undefined extends Violation

  implicit val display: Show[Violation] = Show.shows {
    case p: PathViolation => PathViolation.show(".").shows(p)
    case GreaterThan(expected, actual) => s"'$actual' > '$expected'"
    case LessThan(expected, actual) => s"'$actual' < '$expected'"
    case MemberOf(expected, actual) => s"'$actual' not in $expected"
    case Empty => "non empty"
    case Undefined => "not defined"
  }

}

case class PathViolation(path: Path, violations: NonEmptyList[Violation]) extends Violation

object PathViolation {

  def apply(path: Path, violation: Violation): NonEmptyList[Violation] = violation match {
    case PathViolation(otherPath, violations) =>
      violations.flatMap(v => apply(path ++ otherPath, v))

    case _ => NonEmptyList(PathViolation(path, NonEmptyList(violation)))
  }

  def show(pathSeparator: String): Show[PathViolation] = Show.shows { value =>
    val violationsDesc = value.violations.map(_.show).intercalate1(" and ")
    s"expected $violationsDesc at ${value.path.shows}"
  }

  implicit def jsonWriter: UWriter[PathViolation] = UWriter[PathViolation] {
    pv => Js.Obj(
      "path"       -> Path.pathJsonWriter.write(pv.path),
      "violations" -> implicitly[UWriter[NonEmptyList[Violation]]].write(pv.violations),
      "$type"      -> Js.Str(classOf[PathViolation].getName)
    )
  }

  implicit def jsonReader: UReader[PathViolation] = UReader[PathViolation] {
    val pathReader = Kleisli(Path.pathJsonReader.read.lift)
    val violationsReader = Kleisli(implicitly[UReader[NonEmptyList[Violation]]].read.lift)

    val prod = Kleisli[Option, (Js.Value, Js.Value), PathViolation] { case (path, violations) =>
      (pathReader.run(path) |@| violationsReader.run(violations))((p, vs) => PathViolation(p, vs))
    }

    val extractFieldMap: PartialFunction[Js.Value, Map[String, Js.Value]] = {
      case obj: Js.Obj => obj.value.toMap
    }
    val extractJsValues = Kleisli(extractFieldMap.lift).flatMapK { fields =>
      (fields.get("path") |@| fields.get("violations"))(_ -> _)
    }

    Function.unlift(prod.composeK(extractJsValues).run)
  }

}
