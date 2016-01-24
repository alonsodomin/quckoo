package io.kairos

import upickle.Js
import upickle.default._

import scalaz.{Failure, NonEmptyList, Success, Validation}

/**
  * Created by alonsodomin on 29/12/2015.
  */
package object serialization {
  import scala.language.implicitConversions

  implicit def nonEmptyListW[T: Writer]: Writer[NonEmptyList[T]] = Writer[NonEmptyList[T]] {
    x => Js.Arr(x.list.toList.map(writeJs(_)).toArray: _*)
  }

  implicit def nonEmptyListR[T: Reader]: Reader[NonEmptyList[T]] = Reader[NonEmptyList[T]]( {
    case Js.Arr(x @ _*) =>
      val seq = x.map(readJs[T])
      NonEmptyList(seq.head, seq.tail: _*)
  })

  implicit def validationW[E: Writer, A: Writer]: Writer[Validation[E, A]] = {
    Writer[Validation[E, A]] {
      case Failure(e) => Js.Arr(Js.Num(0), writeJs(e))
      case Success(a) => Js.Arr(Js.Num(1), writeJs(a))
    }
  }

  implicit def successW[E: Writer, A: Writer]: Writer[Success[A]] = Writer[Success[A]](validationW[E, A].write)
  implicit def failureW[E: Writer, A: Writer]: Writer[Failure[E]] = Writer[Failure[E]](validationW[E, A].write)

  implicit def validationR[E: Reader, A: Reader]: Reader[Validation[E, A]] = Reader[Validation[E, A]](
    successR[A].read orElse failureR[E].read
  )

  implicit def successR[A: Reader]: Reader[Success[A]] = Reader[Success[A]] {
    case Js.Arr(Js.Num(1), a) => Success[A](readJs[A](a))
  }
  implicit def failureR[E: Reader]: Reader[Failure[E]] = Reader[Failure[E]] {
    case Js.Arr(Js.Num(0), e) => Failure[E](readJs[E](e))
  }

}
