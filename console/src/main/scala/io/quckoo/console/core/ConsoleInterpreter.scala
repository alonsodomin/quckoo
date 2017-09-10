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

import diode.{ActionHandler, ActionResult, ActionType}

import io.quckoo.client.http.dom.AjaxQuckooClient
import io.quckoo.protocol.Event

import scala.concurrent.ExecutionContext

trait ConsoleInterpreter[S] { this: ActionHandler[ConsoleScope, S] =>

  def handleIO(
      action: ConsoleIO[Event]
  )(implicit ec: ExecutionContext): ActionResult[ConsoleScope] = {
    val session = modelRW.root.zoom(_.session).value

    ActionResult.EffectOnly {
      action
        .run(AjaxQuckooClient)
        .run(session)
        .map { case (newSession, result) => ClientResult(newSession, result) }
    }
  }

}
