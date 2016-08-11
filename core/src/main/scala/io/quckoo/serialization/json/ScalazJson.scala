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
import upickle.default.{Writer => JsonWriter, Reader => JsonReader, _}

import scala.language.implicitConversions
import scalaz._

/**
  * Created by alonsodomin on 19/03/2016.
  */
trait ScalazJson {

  // NonEmptyList

  implicit def nonEmptyListW[T: JsonWriter]: JsonWriter[NonEmptyList[T]] = JsonWriter[NonEmptyList[T]] {
    x => Js.Arr(x.list.toList.map(writeJs(_)).toArray: _*)
  }

  implicit def nonEmptyListR[T: JsonReader]: JsonReader[NonEmptyList[T]] = JsonReader[NonEmptyList[T]]( {
    case Js.Arr(x @ _*) =>
      val seq = x.map(readJs[T])
      NonEmptyList(seq.head, seq.tail: _*)
  })

  // Either \/

  implicit def zeitherW[E: JsonWriter, A: JsonWriter]: JsonWriter[E \/ A] = {
    JsonWriter[E \/ A] {
      case -\/(e) => Js.Arr(Js.Num(10), writeJs(e))
      case \/-(a) => Js.Arr(Js.Num(11), writeJs(a))
    }
  }

  implicit def zleftW[E: JsonWriter, A: JsonWriter]: JsonWriter[-\/[E]] =
    JsonWriter[-\/[E]](zeitherW[E, A].write)

  implicit def zrightW[E: JsonWriter, A: JsonWriter]: JsonWriter[\/-[A]] =
    JsonWriter[\/-[A]](zeitherW[E, A].write)

  implicit def zeitherR[E: JsonReader, A: JsonReader]: JsonReader[E \/ A] =
    JsonReader[E \/ A](zrightR[A].read orElse zleftR[E].read)

  implicit def zleftR[E: JsonReader]: JsonReader[-\/[E]] = JsonReader[-\/[E]] {
    case Js.Arr(Js.Num(10), e) => -\/[E](readJs[E](e))
  }
  implicit def zrightR[A: JsonReader]: JsonReader[\/-[A]] = JsonReader[\/-[A]] {
    case Js.Arr(Js.Num(11), a) => \/-[A](readJs[A](a))
  }

  // These \&/

  implicit def theseW[A: JsonWriter, B: JsonWriter]: JsonWriter[A \&/ B] = JsonWriter[A \&/ B] {
    case \&/.This(a)    => Js.Arr(Js.Num(20), writeJs(a))
    case \&/.That(b)    => Js.Arr(Js.Num(21), writeJs(b))
    case \&/.Both(a, b) => Js.Arr(Js.Num(22), writeJs(a), writeJs(b))
  }

  implicit def thisW[A: JsonWriter, B: JsonWriter]: JsonWriter[\&/.This[A]] =
    JsonWriter[\&/.This[A]](theseW[A, B].write)

  implicit def thatW[A: JsonWriter, B: JsonWriter]: JsonWriter[\&/.That[B]] =
    JsonWriter[\&/.That[B]](theseW[A, B].write)

  implicit def bothW[A: JsonWriter, B: JsonWriter]: JsonWriter[\&/.Both[A, B]] =
    JsonWriter[\&/.Both[A, B]](theseW[A, B].write)

  implicit def theseR[A: JsonReader, B: JsonReader]: JsonReader[A \&/ B] =
    JsonReader[A \&/ B](thisR[A].read orElse thatR[B].read orElse bothR[A, B].read)

  implicit def thisR[A: JsonReader]: JsonReader[\&/.This[A]] = JsonReader[\&/.This[A]] {
    case Js.Arr(Js.Num(20), a) => \&/.This[A](readJs[A](a))
  }
  implicit def thatR[B: JsonReader]: JsonReader[\&/.That[B]] = JsonReader[\&/.That[B]] {
    case Js.Arr(Js.Num(21), b) => \&/.That[B](readJs[B](b))
  }
  implicit def bothR[A: JsonReader, B: JsonReader]: JsonReader[\&/.Both[A, B]] = JsonReader[\&/.Both[A, B]] {
    case Js.Arr(Js.Num(22), a, b) => \&/.Both[A, B](readJs[A](a), readJs[B](b))
  }

  // Validation

  implicit def validationW[E: JsonWriter, A: JsonWriter]: JsonWriter[Validation[E, A]] = {
    JsonWriter[Validation[E, A]] {
      case Failure(e) => Js.Arr(Js.Num(30), writeJs(e))
      case Success(a) => Js.Arr(Js.Num(31), writeJs(a))
    }
  }

  implicit def successW[E: JsonWriter, A: JsonWriter]: JsonWriter[Success[A]] =
    JsonWriter[Success[A]](validationW[E, A].write)
  implicit def failureW[E: JsonWriter, A: JsonWriter]: JsonWriter[Failure[E]] =
    JsonWriter[Failure[E]](validationW[E, A].write)

  implicit def validationR[E: JsonReader, A: JsonReader]: JsonReader[Validation[E, A]] =
    JsonReader[Validation[E, A]](successR[A].read orElse failureR[E].read)

  implicit def successR[A: JsonReader]: JsonReader[Success[A]] = JsonReader[Success[A]] {
    case Js.Arr(Js.Num(31), a) => Success[A](readJs[A](a))
  }
  implicit def failureR[E: JsonReader]: JsonReader[Failure[E]] = JsonReader[Failure[E]] {
    case Js.Arr(Js.Num(30), e) => Failure[E](readJs[E](e))
  }

}
