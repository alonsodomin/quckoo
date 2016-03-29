package io.quckoo.client

import io.quckoo.auth.AuthInfo
import io.quckoo.auth.http._

/**
  * Created by alonsodomin on 26/03/2016.
  */
package object ajax {

  private[ajax] val BaseURI = "/api"
  private[ajax] val LoginURI = BaseURI + "/login"
  private[ajax] val LogoutURI = BaseURI + "/logout"

  private[ajax] val ClusterDetailsURI = BaseURI + "/cluster/info"

  private[ajax] val RegistryBaseURI = BaseURI + "/registry"
  private[ajax] val JobsURI = RegistryBaseURI + "/jobs"
  private[ajax] val RegistryEventsURI = RegistryBaseURI + "/events"

  private[ajax] val SchedulerBaseURI = BaseURI + "/scheduler"
  private[ajax] val ExecutionPlansURI = SchedulerBaseURI + "/plans"

  private[ajax] val JsonRequestHeaders = Map(
    "Content-Type" -> "application/json"
  )

  private[ajax] def authInfo: Option[AuthInfo] =
    Cookie(XSRFTokenCookie).map(AuthInfo(_))

}
