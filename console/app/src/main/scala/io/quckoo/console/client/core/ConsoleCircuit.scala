package io.quckoo.console.client.core

import diode.Implicits.runAfterImpl
import diode._
import diode.data.{AsyncAction, PotMap}
import diode.react.ReactConnector
import io.quckoo.auth.AuthInfo
import io.quckoo.console.client.components.Notification
import io.quckoo.console.client.security.ClientAuth
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.protocol.client.{SignIn, SignOut}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.{ExecutionPlan, JobSpec}

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz.{-\/, OptionT, \/-}

/**
  * Created by alonsodomin on 20/02/2016.
  */
object ConsoleCircuit extends Circuit[ConsoleScope] with ReactConnector[ConsoleScope] with ConsoleOps {

  protected def initialModel: ConsoleScope = ConsoleScope.initial

  override protected def actionHandler = composeHandlers(
    loginHandler,
    foldHandlers(registryHandler, refreshAuthHandler),
    foldHandlers(jobSpecMapHandler, refreshAuthHandler),
    foldHandlers(scheduleHandler, refreshAuthHandler),
    foldHandlers(executionPlanMapHandler, refreshAuthHandler)
  )

  def zoomIntoNotification: ModelRW[ConsoleScope, Option[Notification]] =
    zoomRW(_.notification)((model, notif) => model.copy(notification = notif))

  def zoomIntoAuth: ModelRW[ConsoleScope, Option[AuthInfo]] =
    zoomRW(_.authInfo) { (model, user) => model.copy(authInfo = user) }

  def zoomIntoExecutionPlans: ModelRW[ConsoleScope, PotMap[PlanId, ExecutionPlan]] =
    zoomRW(_.executionPlans) { (model, plans) => model.copy(executionPlans = plans) }

  def zoomIntoJobSpecs: ModelRW[ConsoleScope, PotMap[JobId, JobSpec]] =
    zoomRW(_.jobSpecs) { (model, specs) => model.copy(jobSpecs = specs) }

  val refreshAuthHandler = new ActionHandler(zoomIntoAuth) with ClientAuth {
    override protected def handle = {
      case _ if value.isDefined => updated(authInfo)
    }
  }

  val loginHandler = new ActionHandler(zoomIntoAuth) {
    override def handle = {
      case Login(SignIn(username, password), referral) =>
        effectOnly(Effect(
          ConsoleClient.login(username, password).
            map { authOpt =>
              authOpt.map(auth => LoggedIn(auth, referral)).
                getOrElse(LoginFailed)
            }
        ))

      case SignOut =>
        value.map { implicit auth =>
          effectOnly(Effect(ConsoleClient.logout().map(_ => LoggedOut)))
        } getOrElse ActionResult.NoChange
    }
  }

  val registryHandler = new ActionHandler(zoomIntoNotification)
      with AuthHandler[Option[Notification]] {

    override def handle = {
      case RegisterJob(spec) =>
        handleWithAuth { implicit auth =>
          updated(None, Effect(ConsoleClient.registerJob(spec).map(RegisterJobResult)))
        }

      case RegisterJobResult(validated) =>
        validated.disjunction match {
          case \/-(id) =>
            updated(
              Some(Notification.info(s"Job registered with id $id")),
              Effect.action(RefreshJobSpecs(Set(id))).after(2 seconds)
            )

          case -\/(errors) =>
            updated(Some(Notification.danger(errors)))
        }

      case EnableJob(jobId) =>
        handleWithAuth { implicit auth =>
          updated(None, Effect(ConsoleClient.enableJob(jobId)))
        }

      case DisableJob(jobId) =>
        handleWithAuth { implicit auth =>
          updated(None, Effect(ConsoleClient.disableJob(jobId)))
        }
    }

  }

  val scheduleHandler = new ActionHandler(zoomIntoNotification)
      with AuthHandler[Option[Notification]] {

    override def handle = {
      case msg: ScheduleJob =>
        handleWithAuth { implicit auth: AuthInfo =>
          updated(None, Effect(ConsoleClient.schedule(msg).map(_.fold(identity, identity))))
        }

      case JobNotFound(jobId) =>
        updated(Some(Notification.danger(s"Job not found $jobId")))

      case ExecutionPlanStarted(jobId, planId) =>
        updated(Some(Notification.info(s"Started execution plan for job. planId=$planId")))
    }

  }

  val jobSpecMapHandler = new ActionHandler(zoomIntoJobSpecs)
      with AuthHandler[PotMap[JobId, JobSpec]] {

    override protected def handle = {
      case LoadJobSpecs =>
        handleWithAuth { implicit auth =>
          effectOnly(Effect(loadJobSpecs().map(JobSpecsLoaded)))
        }

      case JobSpecsLoaded(specs) if specs.nonEmpty =>
        updated(PotMap(JobSpecFetcher, specs))

      case JobEnabled(jobId) =>
        effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

      case JobDisabled(jobId) =>
        effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

      case action: RefreshJobSpecs =>
        handleWithAuth { implicit auth =>
          val updateEffect = action.effect(loadJobSpecs(action.keys))(identity)
          action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

  val executionPlanMapHandler = new ActionHandler(zoomIntoExecutionPlans)
      with AuthHandler[PotMap[PlanId, ExecutionPlan]] {

    override protected def handle = {
      case LoadExecutionPlans =>
        handleWithAuth { implicit auth =>
          effectOnly(Effect(loadPlanIds))
        }

      case ExecutionPlanIdsLoaded(ids) =>
        handleWithAuth { implicit auth =>
          effectOnly(Effect(loadPlans(ids).map(ExecutionPlansLoaded)))
        }

      case ExecutionPlansLoaded(plans) if plans.nonEmpty =>
        updated(PotMap(ExecutionPlanFetcher, plans))

      case action: RefreshExecutionPlans =>
        handleWithAuth { implicit auth =>
          val refreshEffect = action.effect(loadPlans(action.keys))(identity)
          action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

}
