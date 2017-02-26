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

package io.quckoo.serialization

import io.quckoo.util.Attempt

import upickle.default.{Reader => UReader, Writer => UWriter, _}

/**
  * Created by alonsodomin on 11/08/2016.
  */
package object json extends ScalazJson with JavaTimeJson with Cron4s {

  type JsonCodec[A] = Codec[A, String]

  implicit def JsonCodecInstance[A: UReader: UWriter]: JsonCodec[A] = new JsonCodec[A] {
    def encode(a: A): Attempt[String] = Attempt(write[A](a))

    def decode(input: String): Attempt[A] = Attempt(read[A](input))
  }

}
