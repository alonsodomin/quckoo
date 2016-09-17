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

import io.quckoo.util.LawfulTry

import upickle.default.{Reader => UReader, Writer => UWriter, _}

import scalaz.ReaderT

/**
  * Created by alonsodomin on 11/08/2016.
  */
package object json extends ScalazJson with JavaTime with Cron4s {
  type JsonReaderT[A] = ReaderT[LawfulTry, String, A]
  type JsonWriterT[A] = ReaderT[LawfulTry, A, String]

  object JsonReaderT {
    def apply[A: UReader]: JsonReaderT[A] = {
      def doRead(str: String): LawfulTry[A] =
        LawfulTry[A](read[A](str))
      ReaderT[LawfulTry, String, A](doRead)
    }
  }

  object JsonWriterT {
    def apply[A: UWriter]: JsonWriterT[A] = {
      def doWrite(a: A): LawfulTry[String] =
        LawfulTry[String](write[A](a))
      ReaderT[LawfulTry, A, String](doWrite)
    }
  }
}
