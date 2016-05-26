package io.quckoo.console.scheduler

import diode.data.PotVector
import diode.react.ModelProxy
import io.quckoo.id.TaskId

/**
  * Created by alonsodomin on 15/05/2016.
  */
object ExecutionList {

  case class Props(proxy: ModelProxy[PotVector[TaskId]])

}
