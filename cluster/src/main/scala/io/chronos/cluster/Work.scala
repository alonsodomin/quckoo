package io.chronos.cluster

import io.chronos.id._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(executionId: ExecutionId,
                moduleId: ModuleId,
                params: Map[String, AnyVal] = Map.empty,
                jobClass: String)