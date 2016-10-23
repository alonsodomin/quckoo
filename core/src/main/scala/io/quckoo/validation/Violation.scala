package io.quckoo.validation

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
}

trait PathViolation extends Violation {
  val path: Path
  val violations: NonEmptyList[Violation]
}

object PathViolation {
  case class PViolation(path: Path, violations: NonEmptyList[Violation]) extends PathViolation

  def apply(path: Path, violation: Violation): NonEmptyList[Violation] = violation match {
    case PViolation(otherPath, violations) =>
      violations.flatMap(v => apply(path ++ otherPath, v))

    case _ => NonEmptyList(PViolation(path, NonEmptyList(violation)))
  }

  def show(pathSeparator: String): Show[PathViolation] = Show.shows { value =>
    val violationsDesc = value.violations.map(_.show).intercalate1(" and ")
    s"expected $violationsDesc at ${value.path.shows}"
  }
}