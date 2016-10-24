package io.quckoo.validation

import io.quckoo.util.IsTraversable

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait TraversableValidators {

  def nonEmptyK[F[_]: Applicative, A](implicit ev: IsTraversable[A]): ValidatorK[F, A] =
    Validator[F, A](a => Applicative[F].pure(ev.subst(a).nonEmpty), _ => Violation.Empty)

  def nonEmpty[A](implicit ev: IsTraversable[A]): Validator[A] = nonEmptyK[Id, A]

}
