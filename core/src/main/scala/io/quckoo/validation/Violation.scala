package io.quckoo.validation

import upickle.Js
import upickle.default.{Reader => UReader, Writer => UWriter, _}

import io.quckoo.serialization.json._

import scalaz._
import Scalaz._

sealed trait Violation

object Violation {
  case class GreaterThan[A](expected: A, actual: A) extends Violation
  case class LessThan[A](expected: A, actual: A) extends Violation

  case object Empty extends Violation
  case object Undefined extends Violation

  implicit val display: Show[Violation] = Show.shows {
    case p: PathViolation => PathViolation.show(".").shows(p)
    case GreaterThan(expected, actual) => s"'$actual' > '$expected'"
    case LessThan(expected, actual) => s"'$actual' < '$expected'"
    case Empty => "non empty"
    case Undefined => "not defined"
  }

  implicit val jsonWriter: UWriter[Violation] = UWriter[Violation] {
    case PathViolation(path, violations) =>
      Js.Obj("path" -> Js.Str(path.shows), "violations" -> implicitly[UWriter[NonEmptyList[Violation]]].write(violations))
    case Empty => Js.Str("EMPTY")
    case Undefined => Js.Str("UNDEFINED")
    case _ => ???
  }

  implicit def jsonReader: UReader[Violation] = UReader[Violation] {
    case Js.Obj(Seq(("path", path), ("violations", violations))) =>
      val pathReader = implicitly[UReader[Path]].read.lift
      val violationsReader = implicitly[UReader[NonEmptyList[Violation]]].read.lift

      // TODO this needs the usage of an Arrow
      //pathReader.andThen(_.flatMap(p => violationsReader.andThen(_.map(vs => PathViolation(p, vs)))))

      /*val parsedPath = Path.unapply(path)
      val parsedNested = implicitly[UReader[NonEmptyList[Violation]]].read.lift(violations)
      parsedPath.flatMap(p => parsedNested.map(vs => PathViolation(p, vs)))*/
      ???

    case _ => ???
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
}