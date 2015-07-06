package io.chronos.scheduler.worker

import io.chronos.scheduler.id.WorkId

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Work(id: WorkId)
case class WorkResult(id: WorkId, result: Any)
