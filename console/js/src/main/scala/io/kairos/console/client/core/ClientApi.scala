package io.kairos.console.client.core

import io.kairos.console.client.security.ClientAuth
import io.kairos.console.info.ClusterInfo
import io.kairos.console.model.Schedule
import io.kairos.console.protocol.LoginRequest
import io.kairos.console.{SchedulerApi, KairosApi, RegistryApi}
import io.kairos.id.{PlanId, JobId}
import io.kairos.protocol.SchedulerProtocol.{ExecutionPlanStarted, JobNotFound, ScheduleJob}
import io.kairos.serialization
import io.kairos.{JobSpec, Validated}
import io.kairos.time.MomentJSDateTime
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
object ClientApi extends KairosApi with RegistryApi with SchedulerApi with ClientAuth {
  import dom.console
  import dom.ext.Ajax
  import serialization.default._
  import serialization.time._

  private[this] val BaseURI = "/api"
  private[this] val LoginURI = BaseURI + "/login"
  private[this] val LogoutURI = BaseURI + "/logout"

  private[this] val ClusterDetailsURI = BaseURI + "/cluster/info"

  private[this] val RegistryBaseURI = BaseURI + "/registry"
  private[this] val JobsURI = RegistryBaseURI + "/jobs"

  private[this] val SchedulerBaseURI = BaseURI + "/scheduler"
  private[this] val ExecutionsURI = SchedulerBaseURI + "/executions"

  private[this] val JsonRequestHeaders = Map("Content-Type" -> "application/json")

  override def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Unit] = {
    import upickle.default._

    Ajax.post(LoginURI, write(LoginRequest(username, password)), headers = JsonRequestHeaders).
      map { xhr => () }
  }

  override def logout()(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.post(LogoutURI, headers = authHeaders) map { xhr => () }
  }

  override def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo] = {
    import upickle.default._

    Ajax.get(ClusterDetailsURI, headers = authHeaders) map { xhr =>
      read[ClusterInfo](xhr.responseText)
    }
  }

  override def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    import upickle.default._

    Ajax.get(JobsURI + "/" + jobId, headers = authHeaders).map { xhr =>
      read[Option[JobSpec]](xhr.responseText)
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    import upickle.default._

    Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      console.log(xhr.responseText)
      read[Validated[JobId]](xhr.responseText)
    }
  }

  override def enabledJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    import upickle.default._

    Ajax.get(JobsURI, headers = authHeaders).map { xhr =>
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Schedule] = {
    import upickle.default._

    Ajax.get(ExecutionsURI + "/" + planId, headers = authHeaders).map { xhr =>
      read[Schedule](xhr.responseText)
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[List[PlanId]] = {
    import upickle.default._

    Ajax.get(ExecutionsURI, headers = authHeaders).map { xhr =>
      read[List[PlanId]](xhr.responseText)
    }
  }

  override def schedule(scheduleJob: ScheduleJob)
                       (implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    import upickle.default._

    Ajax.put(ExecutionsURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Either[JobNotFound, ExecutionPlanStarted]](xhr.responseText)
    }
  }
}
