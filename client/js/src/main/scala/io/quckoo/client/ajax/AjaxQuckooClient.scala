package io.quckoo.client.ajax

import io.quckoo._
import io.quckoo.auth.User
import io.quckoo.auth.http._
import io.quckoo.client.QuckooClient
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization
import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable
import org.reactivestreams.{Publisher, Subscriber}

import org.scalajs.dom.{EventSource, MessageEvent, Event}
import org.scalajs.dom.ext.Ajax

import scalajs.js
import scala.concurrent.{ExecutionContext, Future}

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
    Ajax.post(LogoutURI, headers = authHeaders) map { xhr =>
      // Side effecting here for lacking a better way of doing this
      authToken = None
      ()
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
      Some(read[JobSpec](xhr.responseText))
    } recover {
      case _ => None
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Validated[JobId]](xhr.responseText)
    }
  }

  override def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    println("Loading jobs")
    Ajax.get(JobsURI, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      println(xhr.responseText)
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

  def registryEvents: Observable[RegistryEvent] = {
    val source = new EventSource(WorkerEventsURI)
    Observable.create[RegistryEvent] { subscriber =>

      source.onerror = (event: Event) => {
        if (source.readyState == EventSource.CLOSED) {
          subscriber.onComplete()
        } else {
          subscriber.onError(new Exception(event.toString))
        }
      }

      val listener = (message: MessageEvent) => {
        val sse = read[ServerSentEvent](message.data.toString)
//        val event = read[RegistryEvent](sse.data)
//        subscriber.onNext(event)
        ()
      }

      source.addEventListener[MessageEvent]("RegistryEvent", listener)
    }
  }

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] = {
    Ajax.get(ExecutionPlansURI + "/" + planId, headers = authHeaders).map { xhr =>
      Some(read[ExecutionPlan](xhr.responseText))
    } recover {
      case _ => None
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]] = {
    Ajax.get(ExecutionPlansURI, headers = authHeaders).map { xhr =>
      read[Set[PlanId]](xhr.responseText)
    }
  }

  override def schedule(scheduleJob: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    Ajax.post(ExecutionPlansURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      Right(read[ExecutionPlanStarted](xhr.responseText))
    } recover {
      case _ => Left(JobNotFound(scheduleJob.jobId))
    }
  }

  override def workerEvents: Observable[WorkerEvent] = {
    val source = new EventSource(WorkerEventsURI)
    Observable.create[WorkerEvent] { subscriber =>
      source.onerror = (event: Event) => {
        if (source.readyState == EventSource.CLOSED) {
          subscriber.onComplete()
        } else {
          subscriber.onError(new Exception(event.toString))
        }
      }

      source.addEventListener[MessageEvent]("WorkerEvent", (message: MessageEvent) => {
        val sse = read[ServerSentEvent](message.data.toString)
        val event = read[WorkerEvent](sse.data)
        subscriber.onNext(event)
        ()
      })
    }
  }

}
