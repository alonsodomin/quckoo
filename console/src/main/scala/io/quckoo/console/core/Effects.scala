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

package io.quckoo.console.core

import cats.data.NonEmptyList

import diode._

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 05/07/2016.
  */
private[core] object Effects {

  def seq[E <: Effect](effects: NonEmptyList[E])(implicit ec: ExecutionContext): EffectSeq =
    seq(effects.head, effects.tail: _*)

  def seq(head: Effect, tail: Effect*)(implicit ec: ExecutionContext): EffectSeq =
    new EffectSeq(head, tail, ec)

  def parallel(head: Effect, tail: Effect*)(implicit ec: ExecutionContext): EffectSet =
    new EffectSet(head, tail.toSet, ec)

}
