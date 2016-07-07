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

import io.quckoo.client.QuckooClient
import io.quckoo.client.ajax.AjaxQuckooClientFactory
import io.quckoo.console.components.Notification
import io.quckoo.net.QuckooState
import io.quckoo.time.DateTime

/**
  * Created by alonsodomin on 20/02/2016.
  */

final case class ConsoleScope private (
  client: Option[QuckooClient],
  clusterState: QuckooState,
  userScope: UserScope,
  lastLogin: Option[DateTime]
) {

  def currentUser = client.flatMap(_.principal)

}

object ConsoleScope {

  def initial =
    ConsoleScope(
      client       = AjaxQuckooClientFactory.autoConnect(),
      clusterState = QuckooState(),
      userScope    = UserScope.initial,
      lastLogin    = None
    )

}
