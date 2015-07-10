package io.chronos

import io.chronos.id._

/**
 * Created by aalonsodominguez on 09/07/15.
 */
case class JobSchedule(id: ScheduleId, jobDef: JobDefinition) {

  def isRecurring: Boolean = jobDef.trigger.isRecurring

}
