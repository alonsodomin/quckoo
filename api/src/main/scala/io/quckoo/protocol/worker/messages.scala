package io.quckoo.protocol.worker

import io.quckoo.id.WorkerId

sealed trait WorkerEvent
case class WorkerJoined(workerId: WorkerId) extends WorkerEvent
case class WorkerRemoved(workerId: WorkerId) extends WorkerEvent