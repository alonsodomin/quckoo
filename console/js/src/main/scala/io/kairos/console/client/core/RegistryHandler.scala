package io.kairos.console.client.core

import diode.data._
import diode._
import io.kairos.JobSpec
import io.kairos.id.JobId

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Try}

/**
  * Created by alonsodomin on 06/03/2016.
  */

case object LoadJobSpecs
case class JobSpecsLoaded(value: Map[JobId, JobSpec])

case class UpdateJobSpecs(
    keys: Set[JobId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[JobId, Pot[JobSpec]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[JobId, Pot[JobSpec]], UpdateJobSpecs] {

  override def next(newState: PotState, newValue: Try[Map[JobId, Pot[JobSpec]]]): UpdateJobSpecs =
    copy(state = newState, result = newValue)

}

class JobSpecFetch(dispatch: Dispatcher) extends Fetch[JobId] {
  override def fetch(key: JobId): Unit =
    dispatch(UpdateJobSpecs(keys = Set(key)))

  override def fetch(keys: Traversable[JobId]): Unit =
    dispatch(UpdateJobSpecs(keys.toSet))
}

class RegistryHandler(circuit: Circuit[KairosModel], model: ModelRW[KairosModel, PotMap[JobId, JobSpec]]) extends ActionHandler(model) {

  def loadJobSpec(jobId: JobId): Future[(JobId, Pot[JobSpec])] =
    ClientApi.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs: Future[Map[JobId, JobSpec]] =
    ClientApi.enabledJobs

  def updateJobSpecs(keys: Set[JobId]): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      ClientApi.enabledJobs.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  override def handle = {

    case LoadJobSpecs =>
      effectOnly(Effect(updateJobSpecs(Set.empty)))

    case JobSpecsLoaded(specs) if specs.nonEmpty =>
      val asPotMap = specs.map { case (k, v) => (k, Ready(v)) }
      updated(PotMap(new JobSpecFetch(circuit), asPotMap))

    case action: UpdateJobSpecs =>
      val updateEffect = action.effect(updateJobSpecs(action.keys))(identity)
      action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))

  }

}
