package io.quckoo.client.ajax

import io.quckoo._
import io.quckoo.auth.User
import io.quckoo.client.QuckooClient
import io.quckoo.id._
import io.quckoo.net.ClusterState
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom.ext.{Ajax, AjaxException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * Created by alonsodomin on 26/03/2016.
  */
private[ajax] class AjaxQuckooClient(private var authToken: Option[String]) extends QuckooClient {
  import upickle.default._
  import serialization.json.scalajs._

  private[this] def authHeaders: Map[String, String] =
    authToken.fold(Map.empty[String, String])(token => Map(AuthorizationHeader -> s"Bearer $token"))

  override def principal: Option[User] = authToken.map { token =>
    val jwtClaims = jwtDecodeClaims(token)
    User(jwtClaims("sub"))
  }

  override def close()(implicit ec: ExecutionContext): Future[Unit] =
    Ajax.post(LogoutURI, headers = authHeaders) recover { case _ => () } map { _ =>
      // Side effecting here for lacking a better way of doing this
      authToken = None
      ()
    }

  override def clusterState(implicit ec: ExecutionContext): Future[ClusterState] = {
    withAuthRefresh { () =>
      Ajax.get(ClusterStateURI, headers = authHeaders).map { xhr =>
        read[ClusterState](xhr.responseText)
      }
    }
  }

  override lazy val masterEvents: Observable[MasterEvent] =
    EventSourceObservable[MasterEvent](MasterEventsURI, "master")

  override lazy val workerEvents: Observable[WorkerEvent] =
    EventSourceObservable[WorkerEvent](WorkerEventsURI, "worker")

  override def enableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobEnabled] = {
    withAuthRefresh { () =>
      Ajax.post(JobsURI + "/" + jobId + "/enable", headers = authHeaders).map { xhr =>
        read[JobEnabled](xhr.responseText)
      }
    }
  }

  override def disableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobDisabled] = {
    withAuthRefresh { () =>
      Ajax.post(JobsURI + "/" + jobId + "/disable", headers = authHeaders).map { xhr =>
        read[JobDisabled](xhr.responseText)
      }
    }
  }

  override def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    withAuthRefresh { () =>
      Ajax.get(JobsURI + "/" + jobId, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        Some(read[JobSpec](xhr.responseText))
      }
    } recover {
      case _ => None
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    withAuthRefresh { () =>
      Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        read[Validated[JobId]](xhr.responseText)
      }
    }
  }

  override def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    withAuthRefresh { () =>
      Ajax.get(JobsURI, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        println(xhr.responseText)
        read[Map[JobId, JobSpec]](xhr.responseText)
      }
    }
  }

  def registryEvents: Observable[RegistryEvent] =
    EventSourceObservable[RegistryEvent](RegistryEventsURI, "RegistryEvent")

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] = {
    withAuthRefresh { () =>
      Ajax.get(ExecutionPlansURI + "/" + planId, headers = authHeaders).map { xhr =>
        Some(read[ExecutionPlan](xhr.responseText))
      }
    } recover {
      case _ => None
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]] = {
    withAuthRefresh { () =>
      Ajax.get(ExecutionPlansURI, headers = authHeaders).map { xhr =>
        read[Set[PlanId]](xhr.responseText)
      }
    }
  }

  override def schedule(scheduleJob: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    withAuthRefresh { () =>
      Ajax.post(ExecutionPlansURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
        Right(read[ExecutionPlanStarted](xhr.responseText))
      }
    } recover {
      case _ => Left(JobNotFound(scheduleJob.jobId))
    }
  }

  private[this] def withAuthRefresh[A](action: () => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    action().recoverWith {
      case NonFatal(ajaxEx: AjaxException) if ajaxEx.xhr.status == 401 =>
        Ajax.get(AuthRefreshURI, headers = authHeaders).flatMap(_ => action())
    }
  }

}
