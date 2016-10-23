package io.quckoo

import scalaz._
import Scalaz._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 21/10/2016.
  */
package object validation {
  type ValidatorK[F[_], A] = Kleisli[F, A, ValidationNel[Violation, A]]
  type Validator[A] = ValidatorK[Id, A]

  object Validator {
    def apply[F[_]: Functor, A](test: A => F[Boolean], err: A => Violation): ValidatorK[F, A] = Kleisli { a =>
      test(a).map(cond => if (cond) a.successNel[Violation] else err(a).failureNel[A])
    }

    def accept[F[_], A](implicit ev: Applicative[F]): ValidatorK[F, A] = Kleisli { a => ev.pure(a.successNel[Violation]) }
  }

  object conjunction {
    implicit def semigroup[F[_]: Applicative]: Plus[ValidatorK[F, ?]] = new Plus[ValidatorK[F, ?]] {
      override def plus[A](a: ValidatorK[F, A], b: => ValidatorK[F, A]): ValidatorK[F, A] = Kleisli { x =>
        (a.run(x) |@| b.run(x)) {
          case (l, r) => (l |@| r)((_, out) => out)
        }
      }
    }
  }

  object disjunction {
    implicit def semigroup[F[_]: Applicative]: Plus[ValidatorK[F, ?]] = new Plus[ValidatorK[F, ?]] {
      override def plus[A](a: ValidatorK[F, A], b: => ValidatorK[F, A]): ValidatorK[F, A] = Kleisli { x =>
        (a.run(x) |@| b.run(x)) {
          case (l, r) => l.orElse(r)
        }
      }
    }
  }

  implicit class ValidatorOps[A](self: Validator[A]) {
    def async(implicit ec: ExecutionContext): ValidatorK[Future, A] = self.lift[Future]
  }

  implicit class ValidatorKOps[F[_]: Applicative, A](self: ValidatorK[F, A]) {
    def and(other: ValidatorK[F, A]): ValidatorK[F, A] =
      conjunction.semigroup[F].plus(self, other)
    def &&(other: ValidatorK[F, A]): ValidatorK[F, A] = and(other)

    def or(other: ValidatorK[F, A]): ValidatorK[F, A] =
      disjunction.semigroup[F].plus(self, other)
    def ||(other: ValidatorK[F, A]): ValidatorK[F, A] = or(other)

    def product[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = Kleisli { case (a, b) =>
      (self.run(a) |@| other.run(b))((l, r) => (l |@| r)(_ -> _))
    }
    def *[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = product(other)

    def at(label: String): ValidatorK[F, A] =
      self.map(_.bimap(_.flatMap(v => PathViolation(Path(label), v)), identity))
  }

  implicit class ValidatorK2Ops[F[_]: Applicative, A, B](self: ValidatorK[F, (A, B)]) {
    def product[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = Kleisli { case (a, b, c) =>
      (self.run((a, b)) |@| other.run(c))((l, r) => (l |@| r) { case ((a1, b1), c1) => (a1, b1, c1) })
    }
    def *[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = product(other)
  }

  implicit class ValidatorK3Ops[F[_]: Applicative, A, B, C](self: ValidatorK[F, (A, B, C)]) {
    def product[D](other: ValidatorK[F, D]): ValidatorK[F, (A, B, C, D)] = Kleisli { case (a, b, c, d) =>
      (self.run((a, b, c)) |@| other.run(d))((l, r) => (l |@| r) { case ((a1, b1, c1), d1) => (a1, b1, c1, d1) })
    }
    def *[D](other: ValidatorK[F, D]): ValidatorK[F, (A, B, C, D)] = product(other)
  }

  implicit class ValidatorK4Ops[F[_]: Applicative, A, B, C, D](self: ValidatorK[F, (A, B, C, D)]) {
    def product[E](other: ValidatorK[F, E]): ValidatorK[F, (A, B, C, D, E)] = Kleisli { case (a, b, c, d, e) =>
      (self.run((a, b, c, d)) |@| other.run(e))((l, r) => (l |@| r) { case ((a1, b1, c1, d1), e1) => (a1, b1, c1, d1, e1) })
    }
    def *[E](other: ValidatorK[F, E]): ValidatorK[F, (A, B, C, D, E)] = product(other)
  }

}
