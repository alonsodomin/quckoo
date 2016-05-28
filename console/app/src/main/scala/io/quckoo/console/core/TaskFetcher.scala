package io.quckoo.console.core

import diode.data.Fetch
import io.quckoo.id.TaskId

/**
  * Created by alonsodomin on 28/05/2016.
  */
object TaskFetcher extends Fetch[TaskId] {

  override def fetch(key: TaskId): Unit =
    ConsoleCircuit.dispatch(RefreshTasks(Set(key)))

  override def fetch(keys: Traversable[TaskId]): Unit =
    ConsoleCircuit.dispatch(RefreshTasks(keys.toSet))

}
