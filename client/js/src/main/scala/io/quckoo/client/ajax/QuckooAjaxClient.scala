package io.quckoo.client.ajax

import io.quckoo._
import io.quckoo.auth.AuthInfo
import io.quckoo.auth.http._
import io.quckoo.client.QuckooClient
import io.quckoo.id._
import io.quckoo.protocol.client.SignIn
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.serialization

import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
object QuckooAjaxClient extends QuckooClient {
  import upickle.default._
  import serialization.json.scalajs._

  private[this] val JsonRequestHeaders = Map(
    "Content-Type" -> "application/json"
  )

  private[this] def authHeaders(implicit authInfo: AuthInfo): Map[String, String] =
    Map(XSRFTokenHeader -> authInfo.toString)

  override def authenticate(username: String, password: Array[Char])(implicit ec: ExecutionContext): Future[Option[AuthInfo]] = {
    Ajax.post(LoginURI, write(SignIn(username, password.mkString)), headers = JsonRequestHeaders).
      map {
        _ => Cookie(XSRFTokenCookie).map(AuthInfo(_))
      } recover {
      case _ => None
    }
  }

  override def signOut()(implicit ec: ExecutionContext, auth: AuthInfo): Future[Unit] =
    Ajax.post(LogoutURI, headers = authHeaders) map { xhr => () }

  override def enableJob(jobId: JobId)(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[JobEnabled] = {
    Ajax.post(JobsURI + "/" + jobId + "/enable", headers = authHeaders).map { xhr =>
      read[JobEnabled](xhr.responseText)
    }
  }

  override def disableJob(jobId: JobId)(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[JobDisabled] = {
    Ajax.post(JobsURI + "/" + jobId + "/disable", headers = authHeaders).map { xhr =>
      read[JobDisabled](xhr.responseText)
    }
  }

  override def fetchJob(jobId: JobId)(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[Option[JobSpec]] = {
    Ajax.get(JobsURI + "/" + jobId, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      Some(read[JobSpec](xhr.responseText))
    } recover {
      case _ => None
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[Validated[JobId]] = {
    Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Validated[JobId]](xhr.responseText)
    }
  }

  override def fetchJobs(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[Map[JobId, JobSpec]] = {
    Ajax.get(JobsURI, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[Option[ExecutionPlan]] = {
    Ajax.get(ExecutionPlansURI + "/" + planId, headers = authHeaders).map { xhr =>
      Some(read[ExecutionPlan](xhr.responseText))
    } recover {
      case _ => None
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext, authInfo: AuthInfo): Future[Set[PlanId]] = {
    Ajax.get(ExecutionPlansURI, headers = authHeaders).map { xhr =>
      read[Set[PlanId]](xhr.responseText)
    }
  }

  override def schedule(scheduleJob: ScheduleJob)
                       (implicit ec: ExecutionContext, authInfo: AuthInfo): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    Ajax.post(ExecutionPlansURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      Right(read[ExecutionPlanStarted](xhr.responseText))
    } recover {
      case _ => Left(JobNotFound(scheduleJob.jobId))
    }
  }

}
