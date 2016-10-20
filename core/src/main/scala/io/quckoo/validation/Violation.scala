package io.quckoo.validation

sealed trait Violation

object Violation {
  case class GreaterThan[A](expected: A, actual: A) extends Violation
  case class LessThan[A](expected: A, actual: A) extends Violation

  case object Empty extends Violation
  case object Undefined extends Violation
}