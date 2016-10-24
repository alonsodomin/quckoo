package io.quckoo.validation

import scalaz._
import Scalaz._

import Violation.Undefined

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OptionValidators {

  def definedK[F[_]: Applicative, A]: ValidatorK[F, Option[A]] =
    Validator[F, Option[A]](a => Applicative[F].pure(a.isEmpty), _ => Undefined)

  def defined[A]: Validator[Option[A]] = definedK[Id, A]

}
