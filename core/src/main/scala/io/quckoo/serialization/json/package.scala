/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.serialization

import cats.data.ValidatedNel

import io.circe.{Encoder => CirceEncoder, Decoder => CirceDecoder, Codec => CirceCodec}
import io.circe.parser._
import io.circe.syntax._

import io.quckoo.QuckooError
import io.quckoo.util.Attempt

/**
  * Created by alonsodomin on 11/08/2016.
  */
package object json extends TimeJson {

  type JsonCodec[A]   = Codec[A, String]
  type JsonEncoder[A] = Encoder[A, String]
  type JsonDecoder[A] = Decoder[String, A]

  implicit def errorCodec[A: CirceCodec]: CirceCodec[ValidatedNel[QuckooError, A]] =
    CirceCodec.codecForValidated("error", "success")

  implicit def JsonEncoderInstance[A: CirceEncoder]: JsonEncoder[A] =
    (a: A) => Attempt(a.asJson.noSpaces)

  implicit def JsonDecoderInstance[A: CirceDecoder]: JsonDecoder[A] =
    (input: String) => parse(input).flatMap(_.as[A])

  implicit def JsonCodecInstance[A: CirceDecoder: CirceEncoder]: JsonCodec[A] = new JsonCodec[A] {
    def encode(a: A): Attempt[String] = Attempt(a.asJson.noSpaces)

    def decode(input: String): Attempt[A] = parse(input).flatMap(_.as[A])
  }

}
