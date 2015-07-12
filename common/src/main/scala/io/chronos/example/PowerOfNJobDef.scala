package io.chronos.example

import io.chronos.{JobSchedule, Trigger}

/**
 * Created by aalonsodominguez on 08/07/15.
 */
case class PowerOfNJobDef(n: Int, override val trigger: Trigger = Trigger.Immediate)
  extends JobSchedule(jobId = PowerOfNJobSpec.id, trigger)