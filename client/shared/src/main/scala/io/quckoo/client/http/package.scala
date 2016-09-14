package io.quckoo.client

import io.quckoo.auth.Passport

import scalaz.\/

/**
  * Created by alonsodomin on 04/09/2016.
  */
package object http {

  private[http] val BaseURI = "/api"
  private[http] val LoginURI = BaseURI + "/auth/login"
  private[http] val AuthRefreshURI = BaseURI + "/auth/refresh"
  private[http] val LogoutURI = BaseURI + "/logout"

  private[http] val ClusterStateURI = BaseURI + "/cluster"
  private[http] val MasterEventsURI = ClusterStateURI + "/master"
  private[http] val WorkerEventsURI = ClusterStateURI + "/worker"

  private[http] val RegistryBaseURI = BaseURI + "/registry"
  private[http] val JobsURI = RegistryBaseURI + "/jobs"
  private[http] val RegistryEventsURI = RegistryBaseURI + "/events"

  private[http] val SchedulerBaseURI = BaseURI + "/scheduler"
  private[http] val ExecutionPlansURI = SchedulerBaseURI + "/plans"
  private[http] val TaskExecutionsURI = SchedulerBaseURI + "/executions"
  private[http] val SchedulerEventsURI = SchedulerBaseURI + "/events"

  final val AuthorizationHeader = "Authorization"
  private[http] val JsonRequestHeaders = Map(
    "Content-Type" -> "application/json"
  )

  implicit class HttpPassport(val passport: Passport) extends AnyVal {
    def asHttpHeader: (String, String) = AuthorizationHeader -> s"Bearer ${passport.token}"
  }

}
