package io.kairos.cluster

/**
  * Created by alonsodomin on 28/12/2015.
  */
sealed trait WorkerEvent

case class WorkerRegistered(workerId: WorkerId) extends WorkerEvent
case class WorkerUnregistered(workerId: WorkerId) extends WorkerEvent
