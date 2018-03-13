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

package io.quckoo.client.core

import cats.data.Kleisli

import io.quckoo.util._

import monix.reactive.Observable

/**
  * Created by alonsodomin on 17/09/2016.
  */
private[core] final class TestDriverBackend[P <: Protocol](
    stream: Iterable[P#EventType],
    command: P#Request => Attempt[P#Response]
  ) extends DriverBackend[P] {

  @inline def send =
    Kleisli[Attempt, P#Request, P#Response](command).mapK(attempt2Future)

  @inline def open[Ch <: Channel[P]](channel: Ch) =
    Kleisli[Observable, Unit, P#EventType](_ => Observable.fromIterable(stream))

}
