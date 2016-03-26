package io.quckoo.cluster

import io.quckoo.id.WorkerId

/**
  * Created by alonsodomin on 28/12/2015.
  */
sealed trait WorkerEvent

case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent
