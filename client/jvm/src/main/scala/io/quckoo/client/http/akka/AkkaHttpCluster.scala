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

package io.quckoo.client.http.akka

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}

import io.circe.generic.auto._

import io.quckoo.api2.Cluster
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.net.QuckooState

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

trait AkkaHttpCluster extends AkkaHttpClientSupport with Cluster[ClientIO] {
  import FailFastCirceSupport._

  override def currentState: ClientIO[QuckooState] = ClientIO.auth { session =>
    val request = HttpRequest(HttpMethods.GET, uri = ClusterStateURI)
    sendRequest(request)(handleEntity[QuckooState](_.status == StatusCodes.OK))
  }

}
