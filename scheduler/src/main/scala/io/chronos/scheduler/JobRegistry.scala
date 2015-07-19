package io.chronos.scheduler

import io.chronos.JobSpec
import io.chronos.id._

/**
  * Created by domingueza on 10/07/15.
  */
trait JobRegistry {

   def availableJobSpecs: Seq[JobSpec]

   def specById(jobId: JobId): Option[JobSpec]

   def registerJobSpec(jobSpec: JobSpec): Unit

 }
