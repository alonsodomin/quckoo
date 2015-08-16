package io.chronos.cluster

import java.util.UUID

import io.chronos.id._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(executionId: UUID,
                params: Map[String, Any] = Map.empty,
                moduleId: ModuleId,
                jobClass: String)
