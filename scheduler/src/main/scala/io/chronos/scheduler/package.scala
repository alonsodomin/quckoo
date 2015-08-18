package io.chronos

import io.chronos.cluster.TaskFailureCause

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object scheduler {

  type TaskResult = Either[TaskFailureCause, Any]

  case class TaskMeta(params: Map[String, AnyVal], trigger: Trigger, timeout: Option[FiniteDuration])

}
