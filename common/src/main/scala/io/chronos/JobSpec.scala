package io.chronos

import io.chronos.id._

/**
 * Created by aalonsodominguez on 10/07/15.
 */
class JobSpec(val id: JobId, val displayName: String, val description: String = "", val jobClass: JobClass) extends Serializable
