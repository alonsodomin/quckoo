package io.quckoo.client.free

import io.quckoo.JobSpec
import io.quckoo.auth.Passport
import io.quckoo.fault.JobNotFound
import io.quckoo.id.JobId

import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}

import scala.concurrent.duration.FiniteDuration
import scalaz._

/**
  * Created by domingueza on 03/11/2016.
  */
object registry {

  sealed trait RegistryOp[A] extends QuckooAuthOp[A] {

  }

  object RegistryOp {
    case class FetchJob(jobId: JobId)(implicit val timeout: FiniteDuration, val passport: Passport) extends RegistryOp[Option[JobSpec]]
    case class FetchJobs()(implicit val timeout: FiniteDuration, val passport: Passport) extends RegistryOp[Map[JobId, JobSpec]]
    case class EnableJob(jobId: JobId)(implicit val timeout: FiniteDuration, val passport: Passport) extends RegistryOp[JobNotFound \/ JobEnabled]
    case class DisableJob(jobId: JobId)(implicit val timeout: FiniteDuration, val passport: Passport) extends RegistryOp[JobNotFound \/ JobDisabled]
  }

  type Registry[A] = Free[RegistryOp, A]

  object dsl {
    import RegistryOp._

    def fetchJob(jobId: JobId)(implicit timeout: FiniteDuration, passport: Passport): Registry[Option[JobSpec]] =
      Free.liftF[RegistryOp, Option[JobSpec]](FetchJob(jobId))
    def fetchJobs(implicit timeout: FiniteDuration, passport: Passport): Registry[Map[JobId, JobSpec]] =
      Free.liftF[RegistryOp, Map[JobId, JobSpec]](FetchJobs())
    def enableJob(jobId: JobId)(implicit timeout: FiniteDuration, passport: Passport): Registry[JobNotFound \/ JobEnabled] =
      Free.liftF[RegistryOp, JobNotFound \/ JobEnabled](EnableJob(jobId))
    def disableJob(jobId: JobId)(implicit timeout: FiniteDuration, passport: Passport): Registry[JobNotFound \/ JobDisabled] =
      Free.liftF[RegistryOp, JobNotFound \/ JobDisabled](DisableJob(jobId))
  }

}
