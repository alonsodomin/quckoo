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

import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 01/11/2016.
  */
object syntax extends ValidatorSyntax

trait ValidatorSyntax {

  private def combine[F[_]: Applicative, A](left: ValidatorK[F, A], right: ValidatorK[F, A])(
      implicit ev: Semigroup[Violation]): ValidatorK[F, A] = Kleisli { x =>
    implicit val aSemi = Semigroup.firstSemigroup[A]
    (left.run(x) |@| right.run(x))(_ +++ _)
  }

  object conjunction {
    implicit def instance[F[_]: Applicative]: Plus[ValidatorK[F, ?]] = new Plus[ValidatorK[F, ?]] {
      def plus[A](a: ValidatorK[F, A], b: => ValidatorK[F, A]): ValidatorK[F, A] = {
        import Violation.conjunction._
        combine(a, b)
      }
    }
  }

  object disjunction {
    implicit def instance[F[_]: Applicative]: Plus[ValidatorK[F, ?]] = new Plus[ValidatorK[F, ?]] {
      def plus[A](a: ValidatorK[F, A], b: => ValidatorK[F, A]): ValidatorK[F, A] = {
        import Violation.disjunction._
        combine(a, b)
      }
    }
  }

  implicit class ValidatorOps[A](self: Validator[A]) {
    def async(implicit ec: ExecutionContext): ValidatorK[Future, A] = self.lift[Future]
  }

  implicit class ValidatorKOps[F[_]: Applicative, A](self: ValidatorK[F, A]) {
    def and(other: ValidatorK[F, A]): ValidatorK[F, A] =
      conjunction.instance[F].plus(self, other)
    def &&(other: ValidatorK[F, A]): ValidatorK[F, A] = and(other)

    def or(other: ValidatorK[F, A]): ValidatorK[F, A] =
      disjunction.instance[F].plus(self, other)
    def ||(other: ValidatorK[F, A]): ValidatorK[F, A] = or(other)

    def product[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = Kleisli {
      case (a, b) =>
        import Violation.conjunction._
        (self.run(a) |@| other.run(b))((left, right) => (left |@| right)(_ -> _))
    }
    def *[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = product(other)

    def at(label: String): ValidatorK[F, A] =
      self.map(_.leftMap(v => PathViolation.at(Path(label), v)))
  }

  implicit class ValidatorK2Ops[F[_]: Applicative, A, B](self: ValidatorK[F, (A, B)]) {
    def product[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = Kleisli {
      case (a, b, c) =>
        import Violation.conjunction._
        (self.run((a, b)) |@| other.run(c))((l, r) =>
          (l |@| r) { case ((a1, b1), c1) => (a1, b1, c1) })
    }
    def *[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = product(other)
  }

  implicit class ValidatorK3Ops[F[_]: Applicative, A, B, C](self: ValidatorK[F, (A, B, C)]) {
    def product[D](other: ValidatorK[F, D]): ValidatorK[F, (A, B, C, D)] = Kleisli {
      case (a, b, c, d) =>
        import Violation.conjunction._
        (self.run((a, b, c)) |@| other.run(d))((l, r) =>
          (l |@| r) { case ((a1, b1, c1), d1) => (a1, b1, c1, d1) })
    }
    def *[D](other: ValidatorK[F, D]): ValidatorK[F, (A, B, C, D)] = product(other)
  }

  implicit class ValidatorK4Ops[F[_]: Applicative, A, B, C, D](self: ValidatorK[F, (A, B, C, D)]) {
    def product[E](other: ValidatorK[F, E]): ValidatorK[F, (A, B, C, D, E)] = Kleisli {
      case (a, b, c, d, e) =>
        import Violation.conjunction._
        (self.run((a, b, c, d)) |@| other.run(e))((l, r) =>
          (l |@| r) { case ((a1, b1, c1, d1), e1) => (a1, b1, c1, d1, e1) })
    }
    def *[E](other: ValidatorK[F, E]): ValidatorK[F, (A, B, C, D, E)] = product(other)
  }

}
