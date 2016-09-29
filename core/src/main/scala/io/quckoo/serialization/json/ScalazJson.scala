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

package io.quckoo.serialization.json

import upickle.Js
import upickle.default.{Writer => UWriter, Reader => UReader, _}

import scala.language.implicitConversions
import scalaz._

/**
  * Created by alonsodomin on 19/03/2016.
  */
trait ScalazJson {

  // NonEmptyList

  implicit def nonEmptyListW[T: UWriter]: UWriter[NonEmptyList[T]] = UWriter[NonEmptyList[T]] {
    x => Js.Arr(x.list.toList.map(writeJs(_)).toArray: _*)
  }

  implicit def nonEmptyListR[T: UReader]: UReader[NonEmptyList[T]] = UReader[NonEmptyList[T]]( {
    case Js.Arr(x @ _*) =>
      val seq = x.map(readJs[T])
      NonEmptyList(seq.head, seq.tail: _*)
  })

  // Either \/

  implicit def zeitherW[E: UWriter, A: UWriter]: UWriter[E \/ A] = {
    UWriter[E \/ A] {
      case -\/(e) => Js.Arr(Js.Num(10), writeJs(e))
      case \/-(a) => Js.Arr(Js.Num(11), writeJs(a))
    }
  }

  implicit def zleftW[E: UWriter, A: UWriter]: UWriter[-\/[E]] =
    UWriter[-\/[E]](zeitherW[E, A].write)

  implicit def zrightW[E: UWriter, A: UWriter]: UWriter[\/-[A]] =
    UWriter[\/-[A]](zeitherW[E, A].write)

  implicit def zeitherR[E: UReader, A: UReader]: UReader[E \/ A] =
    UReader[E \/ A](zrightR[A].read orElse zleftR[E].read)

  implicit def zleftR[E: UReader]: UReader[-\/[E]] = UReader[-\/[E]] {
    case Js.Arr(Js.Num(10), e) => -\/[E](readJs[E](e))
  }
  implicit def zrightR[A: UReader]: UReader[\/-[A]] = UReader[\/-[A]] {
    case Js.Arr(Js.Num(11), a) => \/-[A](readJs[A](a))
  }

  // These \&/

  implicit def theseW[A: UWriter, B: UWriter]: UWriter[A \&/ B] = UWriter[A \&/ B] {
    case \&/.This(a)    => Js.Arr(Js.Num(20), writeJs(a))
    case \&/.That(b)    => Js.Arr(Js.Num(21), writeJs(b))
    case \&/.Both(a, b) => Js.Arr(Js.Num(22), writeJs(a), writeJs(b))
  }

  implicit def thisW[A: UWriter, B: UWriter]: UWriter[\&/.This[A]] =
    UWriter[\&/.This[A]](theseW[A, B].write)

  implicit def thatW[A: UWriter, B: UWriter]: UWriter[\&/.That[B]] =
    UWriter[\&/.That[B]](theseW[A, B].write)

  implicit def bothW[A: UWriter, B: UWriter]: UWriter[\&/.Both[A, B]] =
    UWriter[\&/.Both[A, B]](theseW[A, B].write)

  implicit def theseR[A: UReader, B: UReader]: UReader[A \&/ B] =
    UReader[A \&/ B](thisR[A].read orElse thatR[B].read orElse bothR[A, B].read)

  implicit def thisR[A: UReader]: UReader[\&/.This[A]] = UReader[\&/.This[A]] {
    case Js.Arr(Js.Num(20), a) => \&/.This[A](readJs[A](a))
  }
  implicit def thatR[B: UReader]: UReader[\&/.That[B]] = UReader[\&/.That[B]] {
    case Js.Arr(Js.Num(21), b) => \&/.That[B](readJs[B](b))
  }
  implicit def bothR[A: UReader, B: UReader]: UReader[\&/.Both[A, B]] = UReader[\&/.Both[A, B]] {
    case Js.Arr(Js.Num(22), a, b) => \&/.Both[A, B](readJs[A](a), readJs[B](b))
  }

  // Validation

  implicit def validationW[E: UWriter, A: UWriter]: UWriter[Validation[E, A]] = {
    UWriter[Validation[E, A]] {
      case Failure(e) => Js.Arr(Js.Num(30), writeJs[E](e))
      case Success(a) => Js.Arr(Js.Num(31), writeJs[A](a))
    }
  }

  implicit def successW[E: UWriter, A: UWriter]: UWriter[Success[A]] =
    UWriter[Success[A]](validationW[E, A].write)
  implicit def failureW[E: UWriter, A: UWriter]: UWriter[Failure[E]] =
    UWriter[Failure[E]](validationW[E, A].write)

  implicit def validationR[E: UReader, A: UReader]: UReader[Validation[E, A]] =
    UReader[Validation[E, A]](successR[A].read orElse failureR[E].read)

  implicit def successR[A: UReader]: UReader[Success[A]] = UReader[Success[A]] {
    case Js.Arr(Js.Num(31), a) => Success[A](readJs[A](a))
  }
  implicit def failureR[E: UReader]: UReader[Failure[E]] = UReader[Failure[E]] {
    case Js.Arr(Js.Num(30), e) => Failure[E](readJs[E](e))
  }

}
