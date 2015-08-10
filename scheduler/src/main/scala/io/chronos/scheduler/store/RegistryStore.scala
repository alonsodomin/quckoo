package io.chronos.scheduler.store

import io.chronos.JobSpec
import io.chronos.id.JobId
import io.chronos.protocol._

/**
 * Created by aalonsodominguez on 10/08/15.
 */
object RegistryStore {

  def empty: RegistryStore = new RegistryStore(Map.empty)

}

case class RegistryStore private (private val acceptedJobs: Map[JobId, JobSpec]) {

  def get(id: JobId): Option[JobSpec] =
    acceptedJobs.get(id)

  def update(event: RegistryEvent): RegistryStore = event match {
    case JobAccepted(jobId, jobSpec) =>
      copy(acceptedJobs = acceptedJobs + (jobId -> jobSpec))
  }

}
