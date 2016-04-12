package io.quckoo.net

import io.quckoo.protocol.scheduler.TaskQueueUpdated
import monocle.macros.Lenses

/**
  * Created by alonsodomin on 12/04/2016.
  */
@Lenses final case class QuckooMetrics(pendingTasks: Int = 0, inProgressTasks: Int = 0) {

  def updated(event: TaskQueueUpdated): QuckooMetrics =
    copy(pendingTasks = event.pendingTasks, inProgressTasks = event.inProgressTasks)

}
