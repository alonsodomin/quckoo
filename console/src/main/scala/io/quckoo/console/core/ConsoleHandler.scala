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

package io.quckoo.console.core

import diode._

import io.quckoo.client.ClientIO

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 14/05/2017.
  */
abstract class ConsoleHandler[A](modelRW: ModelRW[ConsoleScope, A])
    extends ActionHandler[ConsoleScope, A](modelRW) {

  def toEffect[B: ActionType](
      action: ClientIO[B]
  )(implicit ec: ExecutionContext): Effect =
    Effect(
      action
        .run(modelRW.root.zoom(_.clientState).value)
        .map { case (state, result) => ClientCompleted(state, result) }
        .unsafeToFuture
    )

  def runClientIO[B: ActionType](
      action: ClientIO[B]
  )(implicit ec: ExecutionContext) =
    effectOnly(toEffect(action))

}
