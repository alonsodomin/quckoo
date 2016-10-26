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

import scala.annotation.implicitNotFound

/**
  * Created by alonsodomin on 20/10/2016.
  */
@implicitNotFound("Can not encode ${A} values into ${Out}")
trait Encoder[A, Out] {
  def encode(a: A): Attempt[Out]
}

object Encoder {
  @inline def apply[A, Out](implicit ev: Encoder[A, Out]): Encoder[A, Out] = ev
}
