package io.chronos.scheduler

import io.chronos.JobSpec
import io.chronos.id._

/**
  * Created by domingueza on 10/07/15.
  */
trait JobRepository {

   def availableSpecs: Seq[JobSpec]

   def specById(jobId: JobId): Option[JobSpec]

   def publishSpec(jobSpec: JobSpec): Unit

 }
