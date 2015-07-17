package io.chronos

import io.chronos.id._

/**
 * Created by aalonsodominguez on 10/07/15.
 */
case class JobSpec(id: JobId,
                   displayName: String,
                   description: String = "",
                   moduleId: JobModuleId,
                   jobClass: String)
