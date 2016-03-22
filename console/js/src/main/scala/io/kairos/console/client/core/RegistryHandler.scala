package io.kairos.console.client.core

import diode._
import diode.data._

import io.kairos.JobSpec
import io.kairos.console.client.components.Notification
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz._

/**
  * Created by alonsodomin on 06/03/2016.
  */

class JobSpecsHandler(model: ModelRW[KairosModel, PotMap[JobId, JobSpec]]) extends ActionHandler(model) {
  import RegistryProtocol._

  def loadJobSpec(jobId: JobId): Future[(JobId, Pot[JobSpec])] =
    ConsoleClient.fetchJob(jobId).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty): Future[Map[JobId, Pot[JobSpec]]] = {
    if (keys.isEmpty) {
      ConsoleClient.fetchJobs.map(_.map { case (k, v) => (k, Ready(v)) })
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

class RegistryHandler(model: ModelRW[KairosModel, Option[Notification]]) extends ActionHandler(model) {
  import RegistryProtocol._
  import Implicits._

  override def handle = {
    case RegisterJob(spec) =>
      updated(None, Effect(ConsoleClient.registerJob(spec).map(RegisterJobResult)))

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
      updated(None, Effect(ConsoleClient.enableJob(jobId)))

    case DisableJob(jobId) =>
      updated(None, Effect(ConsoleClient.disableJob(jobId)))
  }

}
