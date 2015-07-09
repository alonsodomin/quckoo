package io.chronos.example

import io.chronos.JobDefinition

/**
 * Created by aalonsodominguez on 08/07/15.
 */
case class PowerOfNJobDef(n: Int) extends JobDefinition(jobId = "Power Of N", jobSpec = classOf[PowerOfNJob])