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

  implicit def greaterThanReader[A: UReader]: UReader[GreaterThan[A]] = UReader[GreaterThan[A]] {
    case Js.Obj(Seq(("expected", expected: Js.Value), ("actual", actual: Js.Value))) =>
      GreaterThan[A](readJs[A](expected), readJs[A](actual))
  }
  implicit def greaterThanWriter[A: UWriter]: UWriter[GreaterThan[A]] = UWriter[GreaterThan[A]] {
    gt => Js.Obj(
      "expected" -> writeJs(gt.expected),
      "actual"   -> writeJs(gt.actual)
    )
  }

  implicit def lessThanReader[A: UReader]: UReader[LessThan[A]] = UReader[LessThan[A]] {
    case Js.Obj(Seq(("expected", expected: Js.Value), ("actual", actual: Js.Value))) =>
      LessThan[A](readJs[A](expected), readJs[A](actual))
  }
  implicit def lessThanWriter[A: UWriter]: UWriter[LessThan[A]] = UWriter[LessThan[A]] {
    lt => Js.Obj(
      "expected" -> writeJs(lt.expected),
      "actual"   -> writeJs(lt.actual)
    )
  }

  /*implicit def jsonWriter: UWriter[Violation] = UWriter[Violation] {
    case PathViolation(path, violations) =>
      Js.Obj(
        "path"       -> implicitly[UWriter[Path]].write(path),
        "violations" -> implicitly[UWriter[NonEmptyList[Violation]]].write(violations)
      )

    case Empty => Js.Str("EMPTY")
    case Undefined => Js.Str("UNDEFINED")
    case _ => ???
  }

  implicit def jsonReader: UReader[Violation] = UReader[Violation] {
    val readPathViolation = {
      val pathReader = Kleisli(implicitly[UReader[Path]].read.lift)
      val violationsReader = Kleisli(implicitly[UReader[NonEmptyList[Violation]]].read.lift)

      val prod = Kleisli[Option, (Js.Value, Js.Value), PathViolation] { case (path, violations) =>
        (pathReader.run(path) |@| violationsReader.run(violations))((p, vs) => PathViolation(p, vs))
      }

      val extractJsValues: PartialFunction[Js.Value, (Js.Value, Js.Value)] = {
        case Js.Obj(Seq(("path", path: Js.Value), ("violations", violations: Js.Value))) => (path, violations)
      }

      Function.unlift(prod.composeK(extractJsValues.lift).run)
    }
    readPathViolation.orElse {
      case _ => ???
    }
  }*/
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
      "path"       -> implicitly[UWriter[Path]].write(pv.path),
      "violations" -> implicitly[UWriter[NonEmptyList[Violation]]].write(pv.violations)
    )
  }

  implicit def jsonReader: UReader[PathViolation] = UReader[PathViolation] {
    val pathReader = Kleisli(implicitly[UReader[Path]].read.lift)
    val violationsReader = Kleisli(implicitly[UReader[NonEmptyList[Violation]]].read.lift)

    val prod = Kleisli[Option, (Js.Value, Js.Value), PathViolation] { case (path, violations) =>
      (pathReader.run(path) |@| violationsReader.run(violations))((p, vs) => PathViolation(p, vs))
    }

    val extractJsValues: PartialFunction[Js.Value, (Js.Value, Js.Value)] = {
      case Js.Obj(Seq(("path", path: Js.Value), ("violations", violations: Js.Value))) => (path, violations)
    }

    Function.unlift(prod.composeK(extractJsValues.lift).run)
  }

}