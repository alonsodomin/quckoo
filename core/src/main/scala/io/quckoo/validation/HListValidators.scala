package io.quckoo.validation

import scalaz._
import shapeless._
import shapeless.labelled._

/**
  * Created by alonsodomin on 24/10/2016.
  */
trait HListValidators {

  implicit def hnilValidator[F[_]: Applicative]: ValidatorK[F, HNil] = Validator.accept

  implicit def hlistValidator[F[_]: Applicative, K <: Symbol, H, T <: HList](
    implicit witness: Witness.Aux[K], hValidator: Lazy[ValidatorK[F, H]], tValidator: ValidatorK[F, T]): ValidatorK[F, FieldType[K, H] :: T] = {
    (hValidator.value.at(witness.value.name) * tValidator).dimap(hlist => (hlist.head, hlist.tail), _.map { case (h, t) => field[K](h) :: t })
  }

  implicit def genericValidator[F[_]: Applicative, A, R](
    implicit gen: LabelledGeneric.Aux[A, R], validator: Lazy[ValidatorK[F, R]]): ValidatorK[F, A] = {
    validator.value.dimap(gen.to, _.map(gen.from))
  }

}
