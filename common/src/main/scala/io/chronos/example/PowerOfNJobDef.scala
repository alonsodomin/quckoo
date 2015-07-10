package io.chronos.example

import io.chronos.JobSchedule

/**
 * Created by aalonsodominguez on 08/07/15.
 */
case class PowerOfNJobDef(n: Int) extends JobSchedule(jobId = PowerOfNJobSpec.id)