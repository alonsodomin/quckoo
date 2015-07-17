package io.chronos

import io.chronos.id.ExecutionId

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(executionId: ExecutionId,
                params: Map[String, Any] = Map.empty,
                moduleId: JobModuleId,
                jobClass: String)
