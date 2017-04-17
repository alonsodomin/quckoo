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

package io.quckoo.client.core

import cats.data.Kleisli

import monix.reactive.Observable

import scala.concurrent.Future

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait DriverBackend[P <: Protocol] {
  def open[Ch <: Channel[P]](channel: Ch): Kleisli[Observable, Unit, P#EventType]
  def send: Kleisli[Future, P#Request, P#Response]
}
