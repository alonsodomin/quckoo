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

package io.quckoo.client

import io.quckoo.auth.XSRFToken
import io.quckoo.auth.http._
import io.quckoo.serialization.Base64._

/**
  * Created by alonsodomin on 26/03/2016.
  */
package object ajax {

  final val AuthorizationHeader = "Authorization"

  private[ajax] val BaseURI = "/api"
  private[ajax] val LoginURI = BaseURI + "/auth/login"
  private[ajax] val AuthRefreshURI = BaseURI + "/auth/refresh"
  private[ajax] val LogoutURI = BaseURI + "/logout"

  private[ajax] val ClusterStateURI = BaseURI + "/cluster"
  private[ajax] val MasterEventsURI = ClusterStateURI + "/master"
  private[ajax] val WorkerEventsURI = ClusterStateURI + "/worker"

  private[ajax] val RegistryBaseURI = BaseURI + "/registry"
  private[ajax] val JobsURI = RegistryBaseURI + "/jobs"
  private[ajax] val RegistryEventsURI = RegistryBaseURI + "/events"

  private[ajax] val SchedulerBaseURI = BaseURI + "/scheduler"
  private[ajax] val ExecutionPlansURI = SchedulerBaseURI + "/plans"
  private[ajax] val TasksURI = SchedulerBaseURI + "/tasks"
  private[ajax] val SchedulerEventsURI = SchedulerBaseURI + "/events"

  private[ajax] val JsonRequestHeaders = Map(
    "Content-Type" -> "application/json"
  )

  private[ajax] def xsrfToken: Option[XSRFToken] =
    Cookie(XSRFTokenCookie).map(XSRFToken(_))

  private[ajax] def jwtDecodeClaims(token: String) = {
    import upickle.default._
    val jwtClaims = token.split('.')(1).toByteArray
    read[Map[String, String]](new String(jwtClaims, "UTF-8"))
  }

}
