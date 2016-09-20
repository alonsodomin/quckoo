package io.quckoo.client

import io.quckoo.auth.Passport

import scalaz.\/

/**
  * Created by alonsodomin on 04/09/2016.
  */
package object http {

  private[http] val ApiURI = "/api"
  private[http] val EventsURI = "/events"

  private[http] val LoginURI = ApiURI + "/auth/login"
  private[http] val AuthRefreshURI = ApiURI + "/auth/refresh"
  private[http] val LogoutURI = ApiURI + "/auth/logout"

  private[http] val ClusterStateURI = ApiURI + "/cluster"
  private[http] val MasterEventsURI = ClusterStateURI + "/master"
  private[http] val WorkerEventsURI = ClusterStateURI + "/worker"

  private[http] val RegistryBaseURI = ApiURI + "/registry"
  private[http] val JobsURI = RegistryBaseURI + "/jobs"
  private[http] val RegistryEventsURI = RegistryBaseURI + "/events"

  private[http] val SchedulerBaseURI = ApiURI + "/scheduler"
  private[http] val ExecutionPlansURI = SchedulerBaseURI + "/plans"
  private[http] val TaskExecutionsURI = SchedulerBaseURI + "/executions"
  private[http] val SchedulerEventsURI = SchedulerBaseURI + "/events"

  final val AuthorizationHeader = "Authorization"
  private[http] val JsonContentTypeHeader = "Content-Type" -> "application/json"

  @inline private[http] def authHeader(passport: Passport): (String, String) =
    AuthorizationHeader -> s"Bearer ${passport.token}"

  implicit val httpCommands = Http

}
