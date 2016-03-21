package io.kairos.console.client.core

import diode._
import diode.data._

import io.kairos.JobSpec
import io.kairos.console.client.components.Notification
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz._

/**
  * Created by alonsodomin on 06/03/2016.
  */

class JobSpecsHandler(model: ModelRW[KairosModel, PotMap[JobId, JobSpec]]) extends ActionHandler(model) {
  import RegistryProtocol._

  def loadJobSpec(jobId: JobId): Future[(JobId, Pot[JobSpec])] =
    ClientApi.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      ClientApi.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) })
    } else {
      Future.sequence(keys.map(loadJobSpec)).map(_.toMap)
    }
  }

  override def handle = {
    case LoadJobSpecs =>
      effectOnly(Effect(loadJobSpecs().map(JobSpecsLoaded)))

    case JobSpecsLoaded(specs) if specs.nonEmpty =>
      updated(PotMap(JobSpecFetcher, specs))

    case JobEnabled(jobId) =>
      effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

    case JobDisabled(jobId) =>
      effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

    case action: RefreshJobSpecs =>
      val updateEffect = action.effect(loadJobSpecs(action.keys))(identity)
      action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))
  }

}

class RegistryHandler(model: ModelRW[KairosModel, KairosModel]) extends ActionHandler(model) {
  import RegistryProtocol._

  override def handle = {
    case RegisterJob(spec) =>
      updated(value.copy(notification = None), Effect(ClientApi.registerJob(spec).map(RegisterJobResult)))

    case RegisterJobResult(validated) =>
      validated.disjunction match {
        case \/-(id) =>
          updated(value.copy(notification = Some(Notification.info(s"Job registered with id $id"))), Effect.action(RefreshJobSpecs(Set(id))))

        case -\/(errors) =>
          updated(value.copy(notification = Some(Notification.danger(errors))))
      }

    case EnableJob(jobId) =>
      updated(value.copy(notification = None), Effect(ClientApi.enableJob(jobId)))

    case DisableJob(jobId) =>
      updated(value.copy(notification = None), Effect(ClientApi.disableJob(jobId)))
  }

}
