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

package io.quckoo.util

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait IsTraversable[A] {
  type Elem
  type T[x] <: Traversable[_]

  def subst(a: A): T[Elem]
}

object IsTraversable {
  def apply[A](implicit ev: IsTraversable[A]): IsTraversable[A] = ev

  implicit def mk[A, E, T0[_] <: Traversable[_]](implicit ev: A => T0[E]): IsTraversable[A] = new IsTraversable[A] {
    type Elem = E
    type T[x] = T0[x]

    def subst(a: A): T[E] = ev(a)
  }
}
