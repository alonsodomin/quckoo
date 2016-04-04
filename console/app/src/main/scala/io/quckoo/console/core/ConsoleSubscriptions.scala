package io.quckoo.console.core

import io.quckoo.client.QuckooClient

import monifu.concurrent.Implicits.globalScheduler

/**
  * Created by alonsodomin on 04/04/2016.
  */
private[core] trait ConsoleSubscriptions {

  def subscribeClusterState(implicit client: QuckooClient): Unit =
    client.workerEvents.subscribe(new WorkerEventSubscriber)

}
