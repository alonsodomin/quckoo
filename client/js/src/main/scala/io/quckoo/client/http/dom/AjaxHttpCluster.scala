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

package io.quckoo.client.http.dom

import cats.Eval
import cats.effect.IO
import cats.implicits._

import io.circe.generic.auto._
import io.circe.parser.decode

import io.quckoo.api2.{Cluster, QuckooIO}
import io.quckoo.client.http._
import io.quckoo.net.QuckooState
import io.quckoo.util._

import org.scalajs.dom.ext.Ajax
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

trait AjaxHttpCluster extends Cluster[QuckooIO] {

  override def currentState: QuckooIO[QuckooState] = QuckooIO.auth { session =>
    val ajax = IO.fromFuture(Eval.later {
      Ajax.get(ClusterStateURI, headers = Map(bearerToken(session.passport)))
    })

    ajax >>= handleResponse {
      case res if res.status == 200 =>
        attempt2IO(decode[QuckooState](res.responseText))
    }
  }

}
