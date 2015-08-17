package io.chronos.cluster

import io.chronos.id._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Task(id: TaskId,
                moduleId: ModuleId,
                params: Map[String, AnyVal] = Map.empty,
                jobClass: String,
                timeout: Option[FiniteDuration] = None)
