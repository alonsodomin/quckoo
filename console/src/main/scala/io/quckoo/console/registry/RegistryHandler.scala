package io.quckoo.console.registry

import diode.{Effect, ModelRW}
import diode.data.{AsyncAction, PotMap}

import io.quckoo.{JobId, JobSpec}
import io.quckoo.console.components.Notification
import io.quckoo.console.core._
import io.quckoo.protocol.registry._

import slogging._

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 14/05/2017.
  */
class RegistryHandler(model: ModelRW[ConsoleScope, PotMap[JobId, JobSpec]], ops: ConsoleOps)(implicit ec: ExecutionContext)
  extends ConsoleHandler[PotMap[JobId, JobSpec]](model)
    with AuthHandler[PotMap[JobId, JobSpec]]
    with LazyLogging {

  override protected def handle = {
    case LoadJobSpecs =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.loadJobSpecs().map(JobSpecsLoaded)))
      }

    case JobSpecsLoaded(specs) if specs.nonEmpty =>
      logger.debug(s"Loaded ${specs.size} job specs from the server.")
      updated(PotMap(JobSpecFetcher, specs))

    case JobAccepted(jobId, spec) =>
      logger.debug(s"Job has been accepted with identifier: $jobId")
      // TODO re-enable following code once registerJob command is fully async
      //val growl = Growl(Notification.info(s"Job accepted: $jobId"))
      //updated(value + (jobId -> Ready(spec)), growl)
      noChange

    case JobEnabled(jobId) =>
      effectOnly(
        Effects.parallel(
          Growl(Notification.info(s"Job enabled: $jobId")),
          RefreshJobSpecs(Set(jobId))
        ))

    case JobDisabled(jobId) =>
      effectOnly(
        Effects.parallel(
          Growl(Notification.info(s"Job disabled: $jobId")),
          RefreshJobSpecs(Set(jobId))
        ))

    case action: RefreshJobSpecs =>
      withAuth { implicit passport =>
        val updateEffect = action.effect(ops.loadJobSpecs(action.keys))(identity)
        action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))
      }

    case RegisterJob(spec) =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.registerJob(spec)))
      }

    case RegisterJobResult(validated) =>
      validated.toEither match {
        case Right(id) =>
          val notification = Notification.info(s"Job registered with id $id")
          val effects = Effects.parallel(
            Growl(notification),
            RefreshJobSpecs(Set(id))
          )
          effectOnly(effects)

        case Left(errors) =>
          val effects = errors.map { err =>
            Notification.danger(err)
          }.map(n => Effect.action(Growl(n)))

          effectOnly(Effects.seq(effects))
      }

    case EnableJob(jobId) =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.enableJob(jobId)))
      }

    case DisableJob(jobId) =>
      withAuth { implicit passport =>
        effectOnly(Effect(ops.disableJob(jobId)))
      }
  }

}
