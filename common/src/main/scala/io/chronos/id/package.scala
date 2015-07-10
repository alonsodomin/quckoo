package io.chronos

/**
 * Created by domingueza on 06/07/15.
 */
package object id {

  type JobId = String
  type ScheduleId = (JobId, Long)
  type ExecutionId = (ScheduleId, Long)
  type WorkSubId = Long

  type WorkerId = String
  type WorkId = (JobId, WorkSubId)

}
