package io.chronos.registry

import akka.cluster.Cluster
import akka.persistence.PersistentView
import io.chronos.JobSpec
import io.chronos.id.JobId
import io.chronos.protocol._

/**
 * Created by aalonsodominguez on 10/08/15.
 */
class EnabledJobsView extends PersistentView {

  private var enabledJobs = Map.empty[JobId, JobSpec]

  override def viewId: String = ???

  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-registry"
    case None       => "registry"
  }

  override def receive: Receive = {
    case JobAccepted(jobId, jobSpec) =>
      enabledJobs += (jobId -> jobSpec)

    case JobDisabled(jobId) =>
      enabledJobs -= jobId

    case GetJob(jobId) =>
      if (enabledJobs.contains(jobId)) {
        sender() ! enabledJobs(jobId)
      } else {
        sender() ! JobNotEnabled(jobId)
      }

    case GetJobs =>
      sender() ! enabledJobs.values
  }

}
