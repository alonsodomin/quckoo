package io.chronos.scheduler.worker

import io.chronos.scheduler.JobSpec
import io.chronos.scheduler.id.WorkId

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(id: WorkId, params: Map[String, Any] = Map.empty, jobSpec: JobSpec)

case class WorkResult(id: WorkId, result: Any)
