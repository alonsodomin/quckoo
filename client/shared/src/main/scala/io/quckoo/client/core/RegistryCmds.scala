package io.quckoo.client.core

import io.quckoo.JobSpec
import io.quckoo.fault._
import io.quckoo.id.JobId
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled, RegisterJob}

import scalaz._

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait RegistryCmds[P <: Protocol] {
  import CmdMarshalling.Auth

  type RegisterJobCmd = Auth[P, RegisterJob, ValidationNel[Fault, JobId]]
  type GetJobCmd      = Auth[P, JobId, Option[JobSpec]]
  type GetJobsCmd     = Auth[P, Unit, Map[JobId, JobSpec]]
  type EnableJobCmd   = Auth[P, JobId, JobNotFound \/ JobEnabled]
  type DisableJobCmd  = Auth[P, JobId, JobNotFound \/ JobDisabled]

  implicit def registerJobCmd: RegisterJobCmd
  implicit def getJobCmd: GetJobCmd
  implicit def getJobsCmd: GetJobsCmd
  implicit def enableJobCmd: EnableJobCmd
  implicit def disableJobCmd: DisableJobCmd
}
