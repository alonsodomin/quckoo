package io.quckoo.console.core

import diode.Implicits.runAfterImpl
import diode._
import diode.data.{AsyncAction, PotMap}
import diode.react.ReactConnector

import io.quckoo.client.QuckooClient
import io.quckoo.client.ajax.AjaxQuckooClientFactory
import io.quckoo.console.components.Notification
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.protocol.cluster.ClusterInfo
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker._
import io.quckoo.{ExecutionPlan, JobSpec}

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz.{-\/, \/-}

/**
  * Created by alonsodomin on 20/02/2016.
  */
object ConsoleCircuit extends Circuit[ConsoleScope] with ReactConnector[ConsoleScope] with ConsoleOps {

  protected def initialModel: ConsoleScope = ConsoleScope.initial

  override protected def actionHandler = composeHandlers(
    loginHandler,
    clusterStateHandler,
    jobSpecMapHandler,
    registryHandler,
    scheduleHandler,
    executionPlanMapHandler
  )

  def zoomIntoNotification: ModelRW[ConsoleScope, Option[Notification]] =
    zoomRW(_.notification)((model, notif) => model.copy(notification = notif))

  def zoomIntoClient: ModelRW[ConsoleScope, Option[QuckooClient]] =
    zoomRW(_.client) { (model, c) => model.copy(client = c) }

  def zoomIntoClusterState: ModelRW[ConsoleScope, ClusterInfo] =
    zoomRW(_.clusterState) { (model, value) => model.copy(clusterState = value) }

  def zoomIntoExecutionPlans: ModelRW[ConsoleScope, PotMap[PlanId, ExecutionPlan]] =
    zoomRW(_.executionPlans) { (model, plans) => model.copy(executionPlans = plans) }

  def zoomIntoJobSpecs: ModelRW[ConsoleScope, PotMap[JobId, JobSpec]] =
    zoomRW(_.jobSpecs) { (model, specs) => model.copy(jobSpecs = specs) }

  val loginHandler = new ActionHandler(zoomIntoClient) {

    override def handle = {
      case Login(username, password, referral) =>
        effectOnly(Effect(
          AjaxQuckooClientFactory.connect(username, password).
            map(client => LoggedIn(client, referral)).
            recover { case _ => LoginFailed }
        ))

      case Logout =>
        value.map { client =>
          effectOnly(Effect(client.close().map(_ => LoggedOut)))
        } getOrElse noChange
    }
  }

  val clusterStateHandler = new ActionHandler(zoomIntoClusterState) {
    import monifu.concurrent.Implicits.globalScheduler

    override def handle = {
      case SubscribeToBackend(client) =>
        client.workerEvents.subscribe(new WorkerEventSubscriber)
        noChange

      case WorkerJoined(workerId) =>
        updated(value.copy(workers = value.workers + 1))

      case WorkerRemoved(workerId) =>
        updated(value.copy(workers = value.workers - 1))
    }

  }

  val registryHandler = new ActionHandler(zoomIntoNotification)
      with ConnectedHandler[Option[Notification]] {

    override def handle = {
      case RegisterJob(spec) =>
        withClient { client =>
          updated(None, Effect(client.registerJob(spec).map(RegisterJobResult)))
        }

      case RegisterJobResult(validated) =>
        validated.disjunction match {
          case \/-(id) =>
            updated(
              Some(Notification.success(s"Job registered with id $id")),
              Effect.action(RefreshJobSpecs(Set(id))).after(2 seconds)
            )

          case -\/(errors) =>
            updated(Some(Notification.danger(errors)))
        }

      case EnableJob(jobId) =>
        withClient { client =>
          updated(None, Effect(client.enableJob(jobId)))
        }

      case DisableJob(jobId) =>
        withClient { client =>
          updated(None, Effect(client.disableJob(jobId)))
        }
    }

  }

  val scheduleHandler = new ActionHandler(zoomIntoNotification)
      with ConnectedHandler[Option[Notification]] {

    override def handle = {
      case msg: ScheduleJob =>
        withClient { client =>
          updated(None, Effect(client.schedule(msg).map(_.fold(identity, identity))))
        }

      case JobNotFound(jobId) =>
        updated(Some(Notification.danger(s"Job not found $jobId")))

      case ExecutionPlanStarted(jobId, planId) =>
        updated(Some(Notification.success(s"Started execution plan for job. planId=$planId")))
    }

  }

  val jobSpecMapHandler = new ActionHandler(zoomIntoJobSpecs)
      with ConnectedHandler[PotMap[JobId, JobSpec]] {

    override protected def handle = {
      case LoadJobSpecs =>
        withClient { implicit client =>
          effectOnly(Effect(loadJobSpecs().map(JobSpecsLoaded)))
        }

      case JobSpecsLoaded(specs) if specs.nonEmpty =>
        updated(PotMap(JobSpecFetcher, specs))

      case JobEnabled(jobId) =>
        effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

      case JobDisabled(jobId) =>
        effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

      case action: RefreshJobSpecs =>
        withClient { implicit client =>
          val updateEffect = action.effect(loadJobSpecs(action.keys))(identity)
          action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

  val executionPlanMapHandler = new ActionHandler(zoomIntoExecutionPlans)
      with ConnectedHandler[PotMap[PlanId, ExecutionPlan]] {

    override protected def handle = {
      case LoadExecutionPlans =>
        withClient { implicit client =>
          effectOnly(Effect(loadPlanIds))
        }

      case ExecutionPlanIdsLoaded(ids) =>
        withClient { implicit client =>
          effectOnly(Effect(loadPlans(ids).map(ExecutionPlansLoaded)))
        }

      case ExecutionPlansLoaded(plans) if plans.nonEmpty =>
        updated(PotMap(ExecutionPlanFetcher, plans))

      case action: RefreshExecutionPlans =>
        withClient { implicit client =>
          val refreshEffect = action.effect(loadPlans(action.keys))(identity)
          action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

}
