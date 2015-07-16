package io

import io.chronos.id.JobId

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 07/07/15.
 */
package object chronos {

  type JobClass = Class[_ <: Job]

  implicit def convertJobIdToSchedule(jobId: JobId): JobSchedule = JobSchedule(jobId)

  implicit def % (jobId: JobId, params: Map[String, Any]): JobSchedule =
    JobSchedule(jobId, params)

  implicit def % (params: Map[String, Any])(implicit jobSchedule: JobSchedule): JobSchedule =
    jobSchedule.copy(params = jobSchedule.params ++ params)

  implicit def % (trigger: Trigger)(implicit jobSchedule: JobSchedule): JobSchedule =
    jobSchedule.copy(trigger = trigger)

  implicit def % (timeout: FiniteDuration)(implicit jobSchedule: JobSchedule): JobSchedule =
    jobSchedule.copy(timeout = Some(timeout))

}
