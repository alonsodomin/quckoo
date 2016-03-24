package io.quckoo.console.client.core

import diode.{ActionHandler, Effect, ModelRW}
import diode.data._
import io.quckoo.ExecutionPlan
import io.quckoo.id.PlanId

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 19/03/2016.
  */
class ExecutionPlanMapHandler(model: ModelRW[ConsoleModel, PotMap[PlanId, ExecutionPlan]])
    extends ActionHandler(model) {

  def loadPlanIds: Future[Set[PlanId]] = ConsoleClient.allExecutionPlanIds

  def loadPlans(ids: Set[PlanId]): Future[Map[PlanId, Pot[ExecutionPlan]]] = {
    Future.sequence(ids.map { id =>
      loadPlan(id).map(plan => id -> plan)
    }) map(_.toMap)
  }

  def loadPlan(id: PlanId): Future[Pot[ExecutionPlan]] =
    ConsoleClient.executionPlan(id) map {
      case Some(plan) => Ready(plan)
      case None       => Unavailable
    } recover {
      case ex => Failed(ex)
    }

  override def handle = {
    case LoadExecutionPlans =>
      effectOnly(Effect(loadPlanIds.map(ExecutionPlanIdsLoaded)))

    case ExecutionPlanIdsLoaded(ids) =>
      effectOnly(Effect(loadPlans(ids).map(ExecutionPlansLoaded)))

    case ExecutionPlansLoaded(plans) if plans.nonEmpty =>
      updated(PotMap(ExecutionPlanFetcher, plans))

    case action: RefreshExecutionPlans =>
      val refreshEffect = action.effect(loadPlans(action.keys))(identity)
      action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
  }

}
