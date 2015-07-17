package io.chronos

import java.util.UUID

/**
 * Created by domingueza on 06/07/15.
 */
package object id {

  type JobId = UUID
  type ScheduleId = (JobId, Long)
  type ExecutionId = (ScheduleId, Long)
  type WorkerId = UUID

  implicit def parseModuleId(moduleId: String): JobModuleId = {
    val parts = moduleId.split(JobModuleId.Separator).map(_.trim)
    JobModuleId(parts(0), parts(1), parts(2))
  }

}
