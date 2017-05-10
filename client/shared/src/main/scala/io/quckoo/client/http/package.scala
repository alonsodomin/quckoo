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

package io.quckoo.client

import io.quckoo.auth.Passport
import io.quckoo.client.core.ProtocolSpecs

/**
  * Created by alonsodomin on 04/09/2016.
  */
package object http {

  private[http] val ApiURI    = "/api"
  private[http] val EventsURI = "/events"

  private[http] val LoginURI       = ApiURI + "/auth/login"
  private[http] val AuthRefreshURI = ApiURI + "/auth/refresh"
  private[http] val LogoutURI      = ApiURI + "/auth/logout"

  private[http] val ClusterStateURI = ApiURI + "/cluster"
  private[http] val MasterEventsURI = ClusterStateURI + "/master"
  private[http] val WorkerEventsURI = ClusterStateURI + "/worker"

  private[http] val RegistryBaseURI   = ApiURI + "/registry"
  private[http] val JobsURI           = RegistryBaseURI + "/jobs"
  private[http] val RegistryEventsURI = RegistryBaseURI + "/events"

  private[http] val SchedulerBaseURI   = ApiURI + "/scheduler"
  private[http] val ExecutionPlansURI  = SchedulerBaseURI + "/plans"
  private[http] val TaskExecutionsURI  = SchedulerBaseURI + "/executions"
  private[http] val SchedulerEventsURI = SchedulerBaseURI + "/events"

  final val AuthorizationHeader           = "Authorization"
  private[http] val JsonContentTypeHeader = "Content-Type" -> "application/json"

  private[http] def topicURI(topicName: String): String =
    EventsURI + "/" + topicName

  @inline private[http] def authHeader(passport: Passport): (String, String) =
    AuthorizationHeader -> s"Bearer ${passport.token}"

  type HttpQuckooClient = QuckooClient[HttpProtocol]

  implicit val httpCommands: ProtocolSpecs[HttpProtocol] = Http

}
