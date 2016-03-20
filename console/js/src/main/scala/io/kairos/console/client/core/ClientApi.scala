package io.kairos.console.client.core

import io.kairos.console.client.security.ClientAuth
import io.kairos.console.info.ClusterInfo
import io.kairos.console.protocol.LoginRequest
import io.kairos.console.{KairosApi, RegistryApi, SchedulerApi}
import io.kairos.id.{JobId, PlanId}
import io.kairos.protocol.RegistryProtocol.{JobDisabled, JobEnabled}
import io.kairos.protocol.SchedulerProtocol.{ExecutionPlanStarted, JobNotFound, ScheduleJob}
import io.kairos.serialization
import io.kairos.{ExecutionPlan, JobSpec, Validated}
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
object ClientApi extends KairosApi with RegistryApi with SchedulerApi with ClientAuth {
  import dom.console
  import dom.ext.Ajax
  import upickle.default._
  import serialization.json.scalajs._

  private[this] val BaseURI = "/api"
  private[this] val LoginURI = BaseURI + "/login"
  private[this] val LogoutURI = BaseURI + "/logout"

  private[this] val ClusterDetailsURI = BaseURI + "/cluster/info"

  private[this] val RegistryBaseURI = BaseURI + "/registry"
  private[this] val JobsURI = RegistryBaseURI + "/jobs"

  private[this] val SchedulerBaseURI = BaseURI + "/scheduler"
  private[this] val ExecutionsURI = SchedulerBaseURI + "/executions"

  private[this] val JsonRequestHeaders = Map(
    "Content-Type" -> "application/json"
  )

  override def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.post(LoginURI, write(LoginRequest(username, password)), headers = JsonRequestHeaders).
      map { xhr => () }
  }

  override def logout()(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.post(LogoutURI, headers = authHeaders) map { xhr => () }
  }

  override def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo] = {
    Ajax.get(ClusterDetailsURI, headers = authHeaders) map { xhr =>
      read[ClusterInfo](xhr.responseText)
    }
  }

  override def enableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobEnabled] = {
    Ajax.post(JobsURI + "/" + jobId + "/enable", headers = authHeaders).map { xhr =>
      read[JobEnabled](xhr.responseText)
    }
  }

  override def disableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobDisabled] = {
    Ajax.post(JobsURI + "/" + jobId + "/disable", headers = authHeaders).map { xhr =>
      read[JobDisabled](xhr.responseText)
    }
  }

  override def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    Ajax.get(JobsURI + "/" + jobId, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      if (xhr.status == 404) None
      else Some(read[JobSpec](xhr.responseText))
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Validated[JobId]](xhr.responseText)
    }
  }

  override def enabledJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    Ajax.get(JobsURI, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[ExecutionPlan] = {
    Ajax.get(ExecutionsURI + "/" + planId, headers = authHeaders).map { xhr =>
      read[ExecutionPlan](xhr.responseText)
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[List[PlanId]] = {
    Ajax.get(ExecutionsURI, headers = authHeaders).map { xhr =>
      read[List[PlanId]](xhr.responseText)
    }
  }

  override def schedule(scheduleJob: ScheduleJob)
                       (implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    Ajax.put(ExecutionsURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Either[JobNotFound, ExecutionPlanStarted]](xhr.responseText)
    }
  }
}
