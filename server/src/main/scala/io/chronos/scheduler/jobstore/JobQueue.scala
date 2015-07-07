package io.chronos.scheduler.jobstore

import java.time.Clock

import io.chronos.JobDefinition

/**
 * Created by domingueza on 07/07/15.
 */
trait JobQueue {

  type JobConsumer = (JobDefinition) => Unit

  def pollOverdueJobs(clock: Clock, batchSize: Int)(implicit consumer: JobConsumer)

  def push(jobDefinition: JobDefinition): Unit

}
