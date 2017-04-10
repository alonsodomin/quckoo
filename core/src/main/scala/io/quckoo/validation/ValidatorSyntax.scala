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
import cats.data.Kleisli
import cats.implicits._

import io.quckoo.util._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 01/11/2016.
  */
object syntax extends ValidatorSyntax

trait ValidatorSyntax {

  object conjunction {
    implicit def instance[F[_]: Applicative]: MonoidK[ValidatorK[F, ?]] = new MonoidK[ValidatorK[F, ?]] {
      def empty[A]: ValidatorK[F, A] = Validator.accept[F, A]

      def combineK[A](a: ValidatorK[F, A], b: ValidatorK[F, A]): ValidatorK[F, A] = Kleisli { x =>
        import Violation.conjunction._
        implicit val semi: Semigroup[A] = (first: A, _: A) => first
        (a.run(x) |@| b.run(x)).map(_ combine _)
      }
    }
  }

  object disjunction {
    implicit def instance[F[_]: Applicative]: MonoidK[ValidatorK[F, ?]] = new MonoidK[ValidatorK[F, ?]] {
      def empty[A]: ValidatorK[F, A] = Validator.reject[F, A]

      def combineK[A](a: ValidatorK[F, A], b: ValidatorK[F, A]): ValidatorK[F, A] = Kleisli { x =>
        import Violation.disjunction._
        implicit val semi: Semigroup[A] = (first: A, _: A) => first
        (a.run(x) |@| b.run(x)).map(_ append _)
      }
    }
  }

  implicit class ValidatorOps[A](self: Validator[A]) {
    def async(implicit ec: ExecutionContext): ValidatorK[Future, A] = self.lift[Future]
  }

  implicit class ValidatorKOps[F[_]: Applicative, A](self: ValidatorK[F, A]) {
    def and(other: ValidatorK[F, A]): ValidatorK[F, A] =
      conjunction.instance[F].combineK(self, other)
    def &&(other: ValidatorK[F, A]): ValidatorK[F, A] = and(other)

    def or(other: ValidatorK[F, A]): ValidatorK[F, A] =
      disjunction.instance[F].combineK(self, other)
    def ||(other: ValidatorK[F, A]): ValidatorK[F, A] = or(other)

    def product[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = Kleisli {
      case (a, b) =>
        import Violation.conjunction._
        (self.run(a) |@| other.run(b)).map((left, right) => (left |@| right).map(_ -> _))
    }
    def *[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = product(other)

    def coproduct[B](other: ValidatorK[F, B]): ValidatorK[F, Either[A, B]] = Kleisli {
      case Left(a) => self.run(a).map(_.map(Left(_)))
      case Right(b) => other.run(b).map(_.map(Right(_)))
    }
    def <*>[B](other: ValidatorK[F, B]): ValidatorK[F, Either[A, B]] = coproduct(other)

    def at(label: String): ValidatorK[F, A] =
      self.map(_.leftMap(v => PathViolation.at(Path(label), v)))
  }

  implicit class ValidatorK2Ops[F[_]: Applicative, A, B](self: ValidatorK[F, (A, B)]) {
    def product[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = Kleisli {
      case (a, b, c) =>
        import Violation.conjunction._
        (self.run((a, b)) |@| other.run(c)).map((l, r) =>
          (l |@| r).map { case ((a1, b1), c1) => (a1, b1, c1) })
    }
    def *[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = product(other)
  }

  implicit class ValidatorK3Ops[F[_]: Applicative, A, B, C](self: ValidatorK[F, (A, B, C)]) {
    def product[D](other: ValidatorK[F, D]): ValidatorK[F, (A, B, C, D)] = Kleisli {
      case (a, b, c, d) =>
        import Violation.conjunction._
        (self.run((a, b, c)) |@| other.run(d)).map((l, r) =>
          (l |@| r).map { case ((a1, b1, c1), d1) => (a1, b1, c1, d1) })
    }
    def *[D](other: ValidatorK[F, D]): ValidatorK[F, (A, B, C, D)] = product(other)
  }

  implicit class ValidatorK4Ops[F[_]: Applicative, A, B, C, D](self: ValidatorK[F, (A, B, C, D)]) {
    def product[E](other: ValidatorK[F, E]): ValidatorK[F, (A, B, C, D, E)] = Kleisli {
      case (a, b, c, d, e) =>
        import Violation.conjunction._
        (self.run((a, b, c, d)) |@| other.run(e)).map((l, r) =>
          (l |@| r).map { case ((a1, b1, c1, d1), e1) => (a1, b1, c1, d1, e1) })
    }
    def *[E](other: ValidatorK[F, E]): ValidatorK[F, (A, B, C, D, E)] = product(other)
  }

}
