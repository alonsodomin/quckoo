package io.chronos.scheduler

import io.chronos.scheduler.id.WorkId

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(id: WorkId, params: Map[String, Any] = Map.empty, jobSpec: JobSpec, workTimeout: Option[FiniteDuration])
