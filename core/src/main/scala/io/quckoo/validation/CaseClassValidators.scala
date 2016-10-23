package io.quckoo.validation

import scalaz.Applicative

/**
  * Created by alonsodomin on 23/10/2016.
  */
trait CaseClassValidators {

  def caseClass2[F[_]: Applicative, T, A, B](aValid: ValidatorK[F, A], bValid: ValidatorK[F, B])
                               (f: T => Option[(A, B)], g: (A, B) => T): ValidatorK[F, T] = {
    (aValid * bValid).dimap(f(_).get, _.map(g.tupled))
  }

  def caseClass3[F[_]: Applicative, T, A, B, C](aValid: ValidatorK[F, A], bValid: ValidatorK[F, B], cValid: ValidatorK[F, C])
                               (f: T => Option[(A, B, C)], g: (A, B, C) => T): ValidatorK[F, T] = {
    (aValid * bValid * cValid).dimap(f(_).get, _.map(g.tupled))
  }

  def caseClass4[F[_]: Applicative, T, A, B, C, D](aValid: ValidatorK[F, A], bValid: ValidatorK[F, B], cValid: ValidatorK[F, C], dValid: ValidatorK[F, D])
                                               (f: T => Option[(A, B, C, D)], g: (A, B, C, D) => T): ValidatorK[F, T] = {
    (aValid * bValid * cValid * dValid).dimap(f(_).get, _.map(g.tupled))
  }

  def caseClass5[F[_]: Applicative, T, A, B, C, D, E](aValid: ValidatorK[F, A], bValid: ValidatorK[F, B], cValid: ValidatorK[F, C], dValid: ValidatorK[F, D], eValid: ValidatorK[F, E])
                                                  (f: T => Option[(A, B, C, D, E)], g: (A, B, C, D, E) => T): ValidatorK[F, T] = {
    (aValid * bValid * cValid * dValid * eValid).dimap(f(_).get, _.map(g.tupled))
  }

}
