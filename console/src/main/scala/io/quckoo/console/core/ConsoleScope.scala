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

import java.time.ZonedDateTime

import io.quckoo.auth.Passport
import io.quckoo.net.QuckooState

import monocle.macros.Lenses

/**
  * Created by alonsodomin on 20/02/2016.
  */
@Lenses final case class ConsoleScope private (
    passport: Option[Passport],
    clusterState: QuckooState,
    userScope: UserScope,
    lastLogin: Option[ZonedDateTime],
    subscribed: Boolean
)

object ConsoleScope {

  def initial =
    ConsoleScope(
      passport = None,
      clusterState = QuckooState(),
      userScope = UserScope.initial,
      lastLogin = None,
      subscribed = false
    )

}
