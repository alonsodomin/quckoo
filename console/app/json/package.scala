import java.util.UUID

import io.chronos.id.{ExecutionId, JobId, ScheduleId}
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by aalonsodominguez on 13/07/15.
 */
package object json {

  implicit def jobIdFormat: Format[JobId] = __.format[UUID]

  implicit def scheduleIdFormat: Format[ScheduleId] = (
      (__ \ "jobId").format[JobId] and
      (__ \ "counter").format[Long]
  )({
    case (jobId, counter) => (jobId, counter)
  }, scheduleId => (scheduleId._1, scheduleId._2))

  implicit def executionIdFormat: Format[ExecutionId] = (
      (__ \ "scheduleId").format[ScheduleId] and
      (__ \ "counter").format[Long]
  )({
    case (scheduleId, counter) => (scheduleId, counter)
  }, executionId => (executionId._1, executionId._2))

}
