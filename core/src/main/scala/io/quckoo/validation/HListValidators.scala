package io.quckoo.validation

import scalaz._
import shapeless._
import shapeless.labelled._

/**
  * Created by alonsodomin on 24/10/2016.
  */
trait HListValidators {

  implicit def hnilValidator[F[_]: Applicative]: ValidatorK[F, HNil] = Validator.accept

  implicit def hlistValidator[F[_]: Applicative, K <: Symbol, H, T <: HList](hValidator: ValidatorK[F, H])(
    implicit witness: Witness.Aux[K], tValidator: ValidatorK[F, T]): ValidatorK[F, FieldType[K, H] :: T] = {
    (hValidator * tValidator).dimap(hlist => (hlist.head, hlist.tail), _.map { case (h, t) => field[K](h) :: t })
  }

  implicit def genericValidator[F[_]: Applicative, A, R](validator: ValidatorK[F, R])(
    implicit gen: LabelledGeneric.Aux[A, R]): ValidatorK[F, A] = {
    def test(a: A) = gen.to(a)
    ???
  }

}
