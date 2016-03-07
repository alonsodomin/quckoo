package io.kairos.console.client.core

import diode.data._
import diode._
import io.kairos.{Validated, JobSpec}
import io.kairos.id.JobId

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Try}
import scalaz._

/**
  * Created by alonsodomin on 06/03/2016.
  */

case object LoadJobSpecs
case class JobSpecsLoaded(value: Map[JobId, Pot[JobSpec]])

case class UpdateJobSpecs(
    keys: Set[JobId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[JobId, Pot[JobSpec]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[JobId, Pot[JobSpec]], UpdateJobSpecs] {

  override def next(newState: PotState, newValue: Try[Map[JobId, Pot[JobSpec]]]): UpdateJobSpecs =
    copy(state = newState, result = newValue)

}

class JobSpecsHandler(model: ModelRW[KairosModel, PotMap[JobId, JobSpec]]) extends ActionHandler(model) {

  override def handle: PartialFunction[AnyRef, ActionResult[KairosModel]] = ???

}

case class RegisterJob(spec: JobSpec)
case class RegisterJobResult(jobId: Validated[JobId])

class RegistryHandler(model: ModelRW[KairosModel, RegistryModel]) extends ActionHandler(model) {

  def loadJobSpec(jobId: JobId): Future[(JobId, Pot[JobSpec])] =
    ClientApi.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      ClientApi.enabledJobs.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  override def handle = {
    case LoadJobSpecs =>
      effectOnly(Effect(loadJobSpecs().map(JobSpecsLoaded)))

    case JobSpecsLoaded(specs) if specs.nonEmpty =>
      updated(value.copy(jobSpecs = PotMap(JobSpecFetch, specs)))

    case RegisterJob(spec) =>
      updated(value.copy(lastErrors = None), Effect(ClientApi.registerJob(spec).map(RegisterJobResult)))

    case RegisterJobResult(validated) =>
      validated.disjunction match {
        case \/-(id) =>
          KairosCircuit.dispatch(UpdateJobSpecs(Set(id)))
          noChange
        case -\/(errors) =>
          updated(value.copy(lastErrors = Some(errors)))
      }

    case action: UpdateJobSpecs =>
      val updateEffect = action.effect(loadJobSpecs(action.keys))(identity)
      val mapModel = model.zoomRW(_.jobSpecs)((model, map) => model.copy(jobSpecs = map))
      action.handleWith(new JobSpecsHandler(mapModel), updateEffect)(AsyncAction.mapHandler(action.keys))

  }

}
