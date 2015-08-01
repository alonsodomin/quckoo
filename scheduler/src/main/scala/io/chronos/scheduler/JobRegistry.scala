package io.chronos.scheduler

import io.chronos.JobSpec
import io.chronos.id._

/**
 * Created by aalonsodominguez on 01/08/15.
 */
trait JobRegistry {

  def getJob(jobId: JobId): Option[JobSpec]

  def registerJob(jobSpec: JobSpec): Unit

  def getJobs: Seq[JobSpec]

}
