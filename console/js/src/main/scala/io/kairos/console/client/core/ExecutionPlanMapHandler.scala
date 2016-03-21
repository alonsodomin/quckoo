package io.kairos.console.client.core

import diode.{ActionHandler, Effect, ModelRW}
import diode.data._
import io.kairos.ExecutionPlan
import io.kairos.id.PlanId

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 19/03/2016.
  */
class ExecutionPlanMapHandler(model: ModelRW[KairosModel, PotMap[PlanId, ExecutionPlan]])
    extends ActionHandler(model) {

  def loadPlanIds: Future[List[PlanId]] = ClientApi.allExecutionPlanIds

  def loadPlans(ids: Set[PlanId]): Future[Map[PlanId, Pot[ExecutionPlan]]] = {
    Future.sequence(ids.map { id =>
      loadPlan(id).map(plan => id -> plan)
    }) map(_.toMap)
  }

  def loadPlan(id: PlanId): Future[Pot[ExecutionPlan]] =
    ClientApi.executionPlan(id) map {
      case Some(plan) => Ready(plan)
      case None       => Unavailable
    }

  override def handle = {
    case LoadExecutionPlans =>
      effectOnly(Effect(loadPlanIds.map(ids => ExecutionPlanIdsLoaded(ids.toSet))))

    case ExecutionPlanIdsLoaded(ids) =>
      effectOnly(Effect(loadPlans(ids).map(ExecutionPlansLoaded)))

    case ExecutionPlansLoaded(plans) if plans.nonEmpty =>
      updated(PotMap(ExecutionPlanFetcher, plans))

    case action: RefreshExecutionPlans =>
      val refreshEffect = action.effect(loadPlans(action.keys))(identity)
      action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
  }

}
