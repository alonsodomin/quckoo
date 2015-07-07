package io.chronos

import io.chronos.id.WorkId

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(id: WorkId, params: Map[String, Any] = Map.empty, jobSpec: JobSpec, timeout: Option[FiniteDuration])
