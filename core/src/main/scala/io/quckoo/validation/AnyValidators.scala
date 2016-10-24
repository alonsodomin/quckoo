package io.quckoo.validation

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 23/10/2016.
  */
trait AnyValidators {

  def anyK[F[_]: Applicative, A]: ValidatorK[F, A] = Validator.accept[F, A]

  def any[A]: Validator[A] = anyK[Id, A]

}
