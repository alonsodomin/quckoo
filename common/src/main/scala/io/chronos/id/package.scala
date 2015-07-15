package io.chronos

import java.util.UUID

/**
 * Created by domingueza on 06/07/15.
 */
package object id {

  type JobId = UUID
  type ScheduleId = (JobId, Long)
  type ExecutionId = (ScheduleId, Long)
  type WorkSubId = Long

  type WorkerId = UUID
  type WorkId = (JobId, WorkSubId)

}
