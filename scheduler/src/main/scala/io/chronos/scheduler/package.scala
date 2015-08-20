package io.chronos

import io.chronos.cluster.TaskFailureCause

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object scheduler {

  type TaskResult = Either[TaskFailureCause, Any]

}
